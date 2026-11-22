package no.nav.common.kafka.util;

import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Intern, trådsikker livssyklus for Kafka-bakgrunnsarbeid som ikke skal tvangsavbrytes.
 *
 * <p>Hver kjøring har egne latcher for stoppsignal og ferdigmelding. Stop setter stoppflagget
 * uten å vente. Start venter på at en stoppet kjøring blir ferdig før nye oppgaver legges inn.
 * Close setter i gang endelig lukking og legger ressurslukking etter arbeidet på den første
 * executoren, også uten forutgående start. Senere start avvises.
 *
 * <p>Tidsgrensen gjelder kallerens venting, ikke bakgrunnsarbeidet. Tidsavbrudd og avbrutt
 * venting meldes som IllegalStateException. Ved avbrutt venting gjenopprettes interrupt-statusen.
 * Kall fra eget arbeid kan sette i gang lukking, men får ikke vente på egen avslutning.
 * Feil i arbeidsoppgaver meldes av stopAndAwait. Feil ved ressurslukking lagres og meldes av close.
 *
 * <p>Arbeidsoppgaver merkes på tråden som utfører dem. Dette gjelder også synkron initialisering
 * og callbacks gjennom inWorker, slik at forsøk på å vente på eget arbeid kan avvises.
 *
 * <p>Ventingen på initialisering og intern executor-avslutning må fullføres selv ved interrupt.
 * Disse hjelpefunksjonene rapporterer avbrudd uten å stoppe kjøringen eller gjenopprette
 * interrupt-statusen. Kalleren bestemmer når stoppsignal og gjenoppretting er riktig.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#shutdown()">Java 21 ordnet shutdown uten tvangsavbrudd</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#awaitTermination(long,java.util.concurrent.TimeUnit)">Java 21 bekreftelse av executor-avslutning</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CountDownLatch.html#await()">Java 21 fullført latch-venting og happens-before</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html#interrupted()">Java 21 avlesing og nullstilling av interrupt-status</a>
 */
@Slf4j
public final class WorkerLifecycle {
    /** Standard maksimal ventetid for omstart og lukking. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private static final ThreadLocal<Boolean> KAFKA_WORKER = new ThreadLocal<>();

    private final List<ExecutorService> executors = new ArrayList<>();
    private final ThreadLocal<Run> workerRun = new ThreadLocal<>();
    private final Runnable terminalAction;
    private final Thread shutdownHook;
    private Run current;
    private boolean closed;
    private volatile Throwable closeFailure;

    /**
     * Kallerens ventefrist for lukking er utløpt, men bakgrunnslukkingen fortsetter.
     * Dette er ikke en feil fra selve ressurslukkingen.
     */
    public static final class CloseTimeoutException extends IllegalStateException {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Beskriver ventefristen som utløp uten å avbryte bakgrunnslukkingen.
         *
         * @param message forklaring på tidsavbruddet
         */
        public CloseTimeoutException(String message) {
            super(message);
        }
    }

    /**
     * Samler signaler og feil for én kjøring, slik at omstart ikke overtar gammel tilstand.
     * En egen privat lås beskytter lagringen av feil, uavhengig av livssykluslåsen.
     */
    private static final class Run {
        private final Object failureLock = new Object();
        private final CountDownLatch stopped = new CountDownLatch(1);
        private final CountDownLatch completed;
        private volatile Throwable failure;

        /**
         * Oppretter ferdigsignalet for arbeidsoppgavene eller en markør for ressurslukking.
         *
         * @param workers antall arbeidsoppgaver som må bli ferdige; 0 brukes for lukkemarkøren
         */
        private Run(int workers) {
            completed = new CountDownLatch(workers);
        }
    }

    /**
     * Ett venteforsøk som bekrefter fullføring eller melder at tråden ble avbrutt.
     *
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#awaitTermination(long,java.util.concurrent.TimeUnit)">Java 21: false betyr at ventingen må fortsette</a>
     */
    @FunctionalInterface
    private interface CompletionWait {
        /**
         * Venter på fullføring eller utløpet av ett venteforsøk.
         *
         * @return true bare når fullføringen er bekreftet
         * @throws InterruptedException hvis ventingen avbrytes
         */
        boolean await() throws InterruptedException;
    }

    /**
     * Venter til initialiseringsforsøket er ferdig, også hvis tråden avbrytes underveis.
     *
     * <p>Tråden må vente til await returnerer normalt, slik at initialiserte verdier blir synlige.
     * Metoden sender ikke stoppsignal og gjenoppretter ikke interrupt-statusen.
     * Returverdien forteller om tråden ble avbrutt. Kalleren må gjenopprette statusen når det er trygt.
     *
     * @param initialized latch som åpnes når initialiseringsforsøket er ferdig
     * @return true hvis interrupt-statusen var satt eller ventingen ble avbrutt; initialiseringsforsøket er alltid ferdig
     *     ved normal retur
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CountDownLatch.html#await()">Java 21 await og happens-before etter countDown</a>
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html#interrupt()">Java 21 gjenoppretting av interrupt-status</a>
     */
    public static boolean awaitInitializationAndReportInterruption(CountDownLatch initialized) {
        return awaitCompletion(() -> {
            initialized.await();
            return true;
        });
    }

    /**
     * Venter på faktisk executor-avslutning uten å tvangsavbryte arbeidsoppgaver.
     *
     * <p>False fra awaitTermination fører til et nytt venteforsøk. Denne interne ventingen har
     * ingen samlet tidsgrense; kallerens stop/close beholder sin begrensede ventetid.
     * Metoden kaller ikke shutdown, sender ikke stoppsignal og gjenoppretter ikke interrupt-statusen.
     * Kalleren må gjenopprette statusen etter at arbeid og ressurslukking er ferdig.
     *
     * @param executor executor der kalleren allerede har kalt shutdown
     * @return true hvis interrupt-statusen var satt eller ventingen ble avbrutt; executoren er alltid avsluttet
     *     ved normal retur
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#awaitTermination(long,java.util.concurrent.TimeUnit)">Java 21 bekreftet avslutning, ikke bare utløpt ventetid</a>
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html#interrupt()">Java 21 gjenoppretting etter ressurslukking</a>
     */
    public static boolean awaitTerminationAndReportInterruption(ExecutorService executor) {
        return awaitCompletion(() -> executor.isTerminated() || executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    /**
     * Gjentar venteforsøk til fullføring er bekreftet, og registrerer om tråden blir avbrutt.
     *
     * <p>Interrupt-statusen registreres og nullstilles før hvert venteforsøk. Nye avbrudd
     * gjenopprettes i catch-blokken og registreres før neste forsøk, uten å avslutte ventingen.
     * Metoden endrer ingen livssyklusflagg. Kalleren gjenoppretter statusen etter ressurslukking.
     *
     * @param wait venteforsøk som bare returnerer true ved faktisk fullføring
     * @return true hvis interrupt-statusen var satt eller et venteforsøk ble avbrutt
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html#interrupted()">Java 21 avlesing og nullstilling av interrupt-status</a>
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CountDownLatch.html#await()">Java 21 await nullstiller interrupt-statusen ved avbrudd</a>
     */
    private static boolean awaitCompletion(CompletionWait wait) {
        boolean interrupted = false;
        boolean completed = false;
        while (!completed) {
            interrupted |= Thread.interrupted();
            try {
                completed = wait.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return interrupted;
    }

    /**
     * Oppretter executorer med én tråd hver, uten å starte arbeid.
     *
     * @param workers antall executorer som livssyklusen skal eie, minst én
     * @param terminalAction ressurslukking som kjøres én gang etter arbeidet på den første executoren
     * @param registerShutdownHook om JVM-avslutning skal kalle close med standardventetiden
     * @throws IllegalStateException hvis en hook registreres etter at JVM-avslutningen har begynt
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Executors.html#newSingleThreadExecutor()">Java 21 sekvensiell oppgavekjøring</a>
     */
    public WorkerLifecycle(int workers, Runnable terminalAction, boolean registerShutdownHook) {
        this(workers, terminalAction, registerShutdownHook, () -> {});
    }

    /**
     * Oppretter executorer og lar shutdown-hooken sende komponentens stoppsignal før venting.
     *
     * @param workers antall executorer som livssyklusen skal eie, minst én
     * @param terminalAction ressurslukking etter pågående arbeid
     * @param registerShutdownHook om JVM-avslutning skal lukke livssyklusen
     * @param shutdownSignal kort, trådsikkert stoppsignal fra shutdown-hooken
     * @throws IllegalStateException hvis en hook registreres etter at JVM-avslutningen har begynt
     */
    public WorkerLifecycle(
            int workers, Runnable terminalAction, boolean registerShutdownHook, Runnable shutdownSignal) {
        this.terminalAction = terminalAction;
        for (int i = 0; i < workers; i++) {
            executors.add(Executors.newSingleThreadExecutor());
        }
        shutdownHook = registerShutdownHook ? new Thread(() -> close(DEFAULT_TIMEOUT, shutdownSignal)) : null;
        if (shutdownHook != null) {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    /**
     * Starter en ny kjøring, eller lar en allerede aktiv kjøring fortsette uendret.
     *
     * <p>Metoden venter inntil ti sekunder på en stoppet kjøring uten å holde livssykluslåsen.
     * En ny kjøring opprettes først når forrige er ferdig. Hvis close kalles under ventingen, avvises start.
     *
     * @param tasks én oppgave per executor som skal brukes i kjøringen
     * @return true hvis en ny kjøring ble lagt inn; false hvis en kjøring allerede var aktiv
     * @throws IllegalArgumentException hvis oppgavelisten er tom eller større enn antall executorer
     * @throws IllegalStateException hvis close er satt i gang, ventingen overskrider tidsgrensen
     *     eller avbrytes, eller kalleren forsøker å vente på eget arbeid
     */
    public boolean start(Runnable... tasks) {
        return startWithWaitGuard(() -> {}, tasks);
    }

    /**
     * Starter som vanlig, men lar komponenten avvise usikker venting før en stoppende kjøring avventes.
     * Kontrollen kjøres uten livssykluslåsen og bare når omstart krever venting.
     *
     * @param waitGuard kontroll som kaster feil hvis kalleren ikke kan vente
     * @param tasks arbeidsoppgavene for en ny kjøring
     * @return true ved ny kjøring; false hvis en kjøring allerede er aktiv
     * @throws IllegalArgumentException hvis oppgavelisten er tom eller større enn antall executorer
     * @throws IllegalStateException hvis lukking, ventekontrollen eller ventingen hindrer start
     */
    public boolean startWithWaitGuard(Runnable waitGuard, Runnable... tasks) {
        if (tasks.length == 0 || tasks.length > executors.size()) {
            throw new IllegalArgumentException("Invalid number of Kafka worker tasks");
        }
        long deadline = deadline(DEFAULT_TIMEOUT);
        while (true) {
            Run previous;
            synchronized (this) {
                if (closed) {
                    throw new IllegalStateException("Cannot start a closed Kafka worker");
                }
                if (current == null || current.completed.getCount() == 0) {
                    Run run = new Run(tasks.length);
                    current = run;
                    for (int i = 0; i < tasks.length; i++) {
                        execute(run, executors.get(i), tasks[i]);
                    }
                    return true;
                }
                if (current.stopped.getCount() != 0) {
                    return false;
                }
                previous = current;
            }
            waitGuard.run();
            await(previous.completed, deadline, "Previous Kafka worker is still stopping", true);
        }
    }

    /**
     * Setter stoppflagget for gjeldende kjøring og vekker pausene uten å vente eller avbryte aktive oppgaver.
     *
     * @see #start(Runnable[])
     */
    public synchronized void stop() {
        if (current != null) {
            current.stopped.countDown();
        }
    }

    /**
     * Sender stoppsignal og venter på den aktuelle kjøringen, ikke på en samtidig omstart.
     *
     * <p>Signalet kalles under livssykluslåsen og må ikke blokkere. Hvis start aldri er kalt,
     * kjøres idleAction på den første executoren. Når kjøringen er ferdig, kan den erstattes.
     * Ved tidsavbrudd fortsetter arbeidet i bakgrunnen. Ved avbrutt venting gjenopprettes
     * interrupt-statusen før feil kastes.
     *
     * @param timeout maksimal ventetid
     * @param signal kort, trådsikkert stoppsignal, for eksempel Kafka-wakeup
     * @param idleAction opprydding før første start, eller null hvis slik opprydding ikke trengs
     * @throws IllegalArgumentException hvis ventetiden er negativ
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting, forsøk på å vente på eget arbeid
     *     eller feil i den stoppede kjøringen
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CountDownLatch.html#await(long,java.util.concurrent.TimeUnit)">Java 21 venting med tidsgrense</a>
     */
    public void stopAndAwait(Duration timeout, Runnable signal, Runnable idleAction) {
        stopAndAwait(timeout, signal, idleAction, true);
    }

    /**
     * Sender stoppsignal og forsøker å vente, men logger tidsavbrudd og avbrutt venting uten å kaste feil.
     *
     * <p>Interrupt-statusen gjenopprettes. Arbeidet fortsetter i bakgrunnen hvis ventingen ikke
     * fullføres. Feil i arbeidet og forsøk på å vente på eget arbeid meldes fortsatt til kalleren.
     *
     * @param timeout maksimal ventetid
     * @param signal kort, trådsikkert stoppsignal
     * @param idleAction opprydding før første start, eller null
     * @see #stopAndAwait(Duration, Runnable, Runnable)
     */
    public void stopAndAwaitBestEffort(Duration timeout, Runnable signal, Runnable idleAction) {
        stopAndAwait(timeout, signal, idleAction, false);
    }

    /**
     * Bruker samme stoppsignal og ferdigmelding for streng venting og best-effort-stopp.
     *
     * @param timeout maksimal ventetid
     * @param signal kort, trådsikkert stoppsignal
     * @param idleAction opprydding før første start, eller null
     * @param failOnWaitError om tidsavbrudd og avbrutt venting skal kaste feil
     */
    private void stopAndAwait(Duration timeout, Runnable signal, Runnable idleAction, boolean failOnWaitError) {
        long deadline = deadline(timeout);
        Run run;
        synchronized (this) {
            if (current == null && !closed && idleAction != null) {
                current = new Run(1);
                current.stopped.countDown();
                execute(current, executors.getFirst(), idleAction);
            }
            stop();
            run = current;
            signal.run();
        }
        if (run != null
                && await(run.completed, deadline, "Kafka worker did not stop before the deadline", failOnWaitError)
                && run.failure != null) {
            throw new IllegalStateException("Kafka worker failed while stopping", run.failure);
        }
    }

    /**
     * Leser stoppflagget for kallerens kjøring, eller gjeldende kjøring for eksterne kallere.
     *
     * @return true når den relevante kjøringen er aktivert og close ikke er satt i gang
     */
    public synchronized boolean isRunning() {
        Run run = workerRun.get();
        if (run == null) {
            run = current;
        }
        return run != null && !closed && run.stopped.getCount() != 0;
    }

    /**
     * Lar en arbeidsoppgave vente til pausen er over eller kjøringen får stoppsignal.
     *
     * <p>Kall uten arbeidsmarkør gjør ingenting. Stoppsignalet avbryter ikke oppgavekode.
     * Det åpner bare latchen som brukes for denne pausen.
     *
     * @param duration maksimal pause
     * @return true ved stoppsignal; false når pausen utløper eller kalleren ikke tilhører en kjøring
     * @throws InterruptedException hvis ventingen avbrytes; kalleren må gjenopprette interrupt-statusen
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CountDownLatch.html#await(long,java.util.concurrent.TimeUnit)">Java 21 venting på stoppsignal</a>
     */
    public boolean pause(Duration duration) throws InterruptedException {
        Run run = workerRun.get();
        // True betyr stoppsignal, false betyr utløpt ventetid. Ingen arbeidsoppgaver avbrytes:
        // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CountDownLatch.html#await(long,java.util.concurrent.TimeUnit)
        return run != null && run.stopped.await(duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * Kjører oppgaven synkront med en arbeidsmarkør for gjeldende kjøring.
     *
     * <p>Brukes ved initialisering og partisjonsbehandling for å oppdage venting på eget arbeid.
     * Tidligere markører gjenopprettes også når oppgaven kaster en feil.
     *
     * @param task oppgaven som skal regnes som arbeid for denne livssyklusen
     */
    public void inWorker(Runnable task) {
        Run run;
        synchronized (this) {
            run = current;
        }
        Run previousRun = workerRun.get();
        workerRun.set(run);
        boolean previousWorker = isWorkerThread();
        KAFKA_WORKER.set(true);
        try {
            task.run();
        } finally {
            if (previousRun == null) {
                workerRun.remove();
            } else {
                workerRun.set(previousRun);
            }
            restoreWorker(previousWorker);
        }
    }

    /**
     * Leser om tråden allerede utfører markert Kafka-arbeid eller initialisering.
     *
     * @return true når tråden er markert for Kafka-arbeid eller initialisering, også for en annen WorkerLifecycle
     */
    public static boolean isWorkerThread() {
        return Boolean.TRUE.equals(KAFKA_WORKER.get());
    }

    /**
     * Setter i gang endelig lukking og venter inntil ti sekunder på arbeid og ressurslukking.
     *
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting, forsøk på å vente på eget arbeid eller lukkefeil
     * @see #close(Duration)
     */
    public void close() {
        close(DEFAULT_TIMEOUT);
    }

    /**
     * Stopper kjøringen og venter på ressurslukking og avslutning av executorene.
     *
     * <p>Pausene vekkes, men aktive arbeidsoppgaver får bli ferdige. Ved tidsavbrudd
     * fortsetter arbeidet og ressurslukkingen i bakgrunnen.
     *
     * @param timeout maksimal ventetid for alle eide executorer og ressurslukking
     * @throws IllegalArgumentException hvis ventetiden er negativ
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting, forsøk på å vente på eget arbeid eller lukkefeil
     * @see #close(Duration, Runnable)
     */
    public void close(Duration timeout) {
        close(timeout, () -> {});
    }

    /**
     * Avviser senere start, sender stoppsignal og venter på at alle executorene avsluttes.
     *
     * <p>Signalet må være kort og ikke blokkere. Ressurslukking utføres én gang etter arbeidet på
     * den første executoren. Gjentatte kall venter på samme lukking og melder lagrede lukkefeil.
     * Ved avbrutt venting gjenopprettes interrupt-statusen uten å avbryte bakgrunnsoppgavene.
     * Kall fra eget arbeid starter lukking, men får feil i stedet for å vente.
     * Bruk en annen tråd for å bekrefte at alt er avsluttet.
     *
     * @param timeout maksimal samlet ventetid
     * @param signal kort, trådsikkert stoppsignal som sendes ved første lukking
     * @throws IllegalArgumentException hvis ventetiden er negativ
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting, forsøk på å vente på eget arbeid
     *     eller feil fra stoppsignalet eller ressurslukkingen
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#awaitTermination(long,java.util.concurrent.TimeUnit)">Java 21: false betyr at executoren ikke er avsluttet</a>
     */
    public void close(Duration timeout, Runnable signal) {
        long deadline = deadline(timeout);
        shutdown(signal);
        checkNotWorker();
        try {
            // shutdown venter ikke. Bare true fra awaitTermination bekrefter fullført avslutning:
            // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#awaitTermination(long,java.util.concurrent.TimeUnit)
            for (ExecutorService executor : executors) {
                if (!executor.awaitTermination(remaining(deadline), TimeUnit.NANOSECONDS)) {
                    throw new CloseTimeoutException("Kafka worker did not close before the deadline");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while closing Kafka worker", e);
        }
        if (closeFailure != null) {
            throw new IllegalStateException("Failed to close Kafka worker resources", closeFailure);
        }
    }

    /**
     * Starter endelig lukking uten å vente, også ved kall fra eget arbeid.
     *
     * <p>Bruk close fra en annen tråd for å vente på avslutningen og få eventuelle lukkefeil.
     *
     * @see #close(Duration)
     */
    public void shutdown() {
        shutdown(() -> {});
    }

    /**
     * Starter lukking én gang og legger ressurslukking etter arbeidet på den første executoren.
     *
     * <p>Et kall til {@code shutdown()} lar oppgavene i køen bli ferdige.
     * {@code shutdownNow()} og {@code ExecutorService.close()} brukes ikke, siden de kan avbryte arbeid.
     * Hooken fjernes hvis JVM-avslutningen ikke har begynt.
     *
     * @param signal kort stoppsignal under livssykluslåsen; feil lagres for eksplisitt close
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#shutdown()">Java 21 shutdown bevarer køen</a>
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#close()">Java 21 close kaller shutdownNow hvis ventingen avbrytes</a>
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runtime.html#removeShutdownHook(java.lang.Thread)">Java 21 avregistrering av shutdown hook</a>
     */
    private void shutdown(Runnable signal) {
        synchronized (this) {
            if (!closed) {
                closed = true;
                stop();
                runClosingAction(signal);
                executors.getFirst().execute(() -> {
                    // Kjører etter siste arbeidsoppgave, også når start aldri ble kalt.
                    workerRun.set(new Run(0));
                    try {
                        runClosingAction(terminalAction);
                    } finally {
                        workerRun.remove();
                    }
                });
                // shutdown lar køen bli ferdig. shutdownNow og ExecutorService.close kan avbryte arbeid:
                // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#shutdown()
                // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#close()
                executors.forEach(ExecutorService::shutdown);
                if (shutdownHook != null) {
                    try {
                        Runtime.getRuntime().removeShutdownHook(shutdownHook);
                    } catch (IllegalStateException ignored) {
                        // JVM-en har allerede startet avslutningen.
                        // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runtime.html#removeShutdownHook(java.lang.Thread)
                    }
                }
            }
        }
    }

    /**
     * Legger inn en oppgave som lagrer feil for kjøringen og markerer oppgaven som ferdig i finally-blokken.
     *
     * @param run kjøringen oppgaven tilhører
     * @param executor executoren som eier oppgaven
     * @param task arbeidet som skal utføres uten tvangsavbrudd
     */
    private void execute(Run run, ExecutorService executor, Runnable task) {
        executor.execute(() -> {
            workerRun.set(run);
            boolean previousWorker = isWorkerThread();
            KAFKA_WORKER.set(true);
            try {
                Throwable failure = runCapturingFailure(task);
                if (failure != null) {
                    recordRunFailure(run, failure);
                    rethrowWorkerFailure(failure);
                }
            } finally {
                workerRun.remove();
                restoreWorker(previousWorker);
                run.completed.countDown();
                if (run.completed.getCount() == 0) {
                    run.stopped.countDown();
                }
            }
        });
    }

    /**
     * Kjører på samme tråd og henter oppgavefeilen etter fullføring.
     *
     * @param action arbeidet som skal fullføres før resultatet leses
     * @return oppgavens feil, eller null ved vellykket fullføring
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/FutureTask.html#run()">Java 21 FutureTask.run</a>
     * @see Future#exceptionNow()
     */
    public static Throwable runCapturingFailure(Runnable action) {
        FutureTask<Void> task = new FutureTask<>(() -> {
            action.run();
            return null;
        });
        task.run();
        return task.state() == Future.State.SUCCESS ? null : task.exceptionNow();
    }

    /**
     * Lagrer lukkefeil for senere bekreftet lukking, også når en callback kaster Error.
     *
     * @param action stoppsignal eller ressurslukking som utføres på kallerens tråd
     */
    private void runClosingAction(Runnable action) {
        Throwable failure = runCapturingFailure(action);
        if (failure != null) {
            recordCloseFailure(failure);
        }
    }

    /**
     * Sender uventede arbeiderfeil videre etter at feilen er lagret for kjøringen.
     *
     * @param failure feilen som skal sendes videre uten å endre RuntimeException eller Error
     */
    private static void rethrowWorkerFailure(Throwable failure) {
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException("Kafka worker failed", failure);
    }

    /**
     * Lagrer første feil i en arbeidsoppgave for kjøringen, uavhengig av senere omstart.
     *
     * <p>Kjøringens private lås beskytter første feil og tilhørende suppressed exceptions.
     * Metoden tar ikke livssykluslåsen.
     *
     * @param run kjøringen der oppgaven feilet
     * @param failure feilen som skal meldes av stopAndAwait
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.1">Java 21 synkronisering på et objekts monitor</a>
     */
    private static void recordRunFailure(Run run, Throwable failure) {
        synchronized (run.failureLock) {
            if (run.failure == null) {
                run.failure = failure;
            } else if (run.failure != failure) {
                run.failure.addSuppressed(failure);
            }
        }
    }

    /**
     * Gjenoppretter den globale arbeidsmarkøren etter at et kall er ferdig, også ved nestede kall.
     *
     * @param previousWorker om tråden var markert for Kafka-arbeid før kallet
     */
    private static void restoreWorker(boolean previousWorker) {
        if (previousWorker) {
            KAFKA_WORKER.set(true);
        } else {
            KAFKA_WORKER.remove();
        }
    }

    /**
     * Lagrer første lukkefeil og legger senere, ulike feil til som suppressed exceptions.
     *
     * @param failure feil fra stoppsignal eller ressurslukking
     */
    private synchronized void recordCloseFailure(Throwable failure) {
        if (closeFailure == null) {
            closeFailure = failure;
        } else if (closeFailure != failure) {
            closeFailure.addSuppressed(failure);
        }
    }

    /**
     * Bekrefter fullføring, eller logger mislykket best-effort-venting uten å skjule arbeiderfeil.
     *
     * @param latch ferdigsignalet som må nå null
     * @param deadline absolutt tidsgrense basert på System.nanoTime
     * @param message feilmelding ved tidsavbrudd
     * @param failOnWaitError om mislykket venting skal kaste feil
     * @return true bare når ferdigsignalet er bekreftet
     */
    private boolean await(CountDownLatch latch, long deadline, String message, boolean failOnWaitError) {
        checkNotWorker();
        try {
            if (latch.await(remaining(deadline), TimeUnit.NANOSECONDS)) {
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (failOnWaitError) {
                throw new IllegalStateException("Interrupted while waiting for Kafka worker", e);
            }
            log.error("Interrupted while waiting for Kafka worker to stop; work continues in the background", e);
            return false;
        }
        if (failOnWaitError) {
            throw new IllegalStateException(message);
        }
        log.warn("{}; work continues in the background", message);
        return false;
    }

    /**
     * Avviser venting fra arbeid eller ressurslukking som tilhører denne livssyklusen.
     *
     * @throws IllegalStateException hvis kalleren ville vente på egen avslutning
     */
    private void checkNotWorker() {
        if (workerRun.get() != null) {
            throw new IllegalStateException("A Kafka worker cannot wait for its own termination");
        }
    }

    /**
     * Regner ventetiden om til en absolutt tidsgrense basert på nanoTime.
     *
     * @param timeout ikke-negativ ventetid
     * @return absolutt tidsgrense for den samlede ventingen
     * @throws IllegalArgumentException hvis ventetiden er negativ
     * @throws ArithmeticException hvis varigheten ikke kan representeres i nanosekunder
     */
    private static long deadline(Duration timeout) {
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        return System.nanoTime() + timeout.toNanos();
    }

    /**
     * Regner ut gjenværende ventetid uten å returnere en negativ verdi.
     *
     * @param deadline absolutt nanoTime-tidsgrense
     * @return gjenværende nanosekunder, eller 0 når tidsgrensen er passert
     */
    private static long remaining(long deadline) {
        return Math.max(0, deadline - System.nanoTime());
    }
}
