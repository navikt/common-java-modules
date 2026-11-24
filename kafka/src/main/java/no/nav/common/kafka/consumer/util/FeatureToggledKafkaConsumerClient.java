package no.nav.common.kafka.consumer.util;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.util.WorkerLifecycle;
import no.nav.common.kafka.util.WorkerLifecycle.CloseTimeoutException;

/**
 * Kafka-klient som stoppes og startes etter en feature-toggle.
 *
 * <p>Start vurderer togglen synkront på kallerens tråd. Senere vurderinger kjøres med fast
 * periode på én bakgrunnstråd uten overlapp. Stop forsøker å vente på at toggle-tråden stopper klienten.
 * Wrapperen kan startes igjen etter stop. Close venter på endelig lukking av den underliggende
 * klienten og frigjør wrapperens executor. Senere start avvises.
 *
 * <p>Stop logger tidsavbrudd og avbrutt venting. Close melder slike feil til kalleren.
 * Arbeidet fortsetter i bakgrunnen, og interrupt-statusen
 * gjenopprettes. Consumer-callbacks får ikke vente på wrapperen, også under initialisering.
 * Direkte livssyklusdelegering til den underliggende klienten tillates.
 * Dette hindrer at en consumer-callback venter på en klient som selv venter på callbacken.
 *
 * @see KafkaConsumerClient
 * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#multithreaded">Kafka 4.4 consumer-eierskap og callback-tråder</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ScheduledExecutorService.html#scheduleAtFixedRate(java.lang.Runnable,long,long,java.util.concurrent.TimeUnit)">Java 21 fast periode uten samtidige callbacks</a>
 */
@Slf4j
public class FeatureToggledKafkaConsumerClient implements KafkaConsumerClient {
    private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofMinutes(1);
    private static final String UNSAFE_WAIT_MESSAGE = "A Kafka worker cannot wait for feature toggle termination";
    private static final ThreadLocal<KafkaConsumerClient> DELEGATED_CLIENT = new ThreadLocal<>();

    private final KafkaConsumerClient kafkaConsumerClient;
    private final Duration pollTimeoutDuration;
    private final Supplier<Boolean> isConsumerToggledOffSupplier;
    private final WorkerLifecycle lifecycle;

    /**
     * Oppretter wrapperen med ett minutt mellom planlagte toggle-vurderinger, uten å starte arbeid.
     *
     * @param kafkaConsumerClient klienten som skal startes, stoppes og lukkes
     * @param isToggledOffSupplier leverer true for pause og false for kjøring
     */
    public FeatureToggledKafkaConsumerClient(
            KafkaConsumerClient kafkaConsumerClient, Supplier<Boolean> isToggledOffSupplier) {
        this(kafkaConsumerClient, isToggledOffSupplier, DEFAULT_POLL_TIMEOUT);
    }

    /**
     * Oppretter wrapperen med en valgt periode mellom toggle-vurderingene.
     *
     * @param kafkaConsumerClient klienten som wrapperens close til slutt lukker
     * @param isToggledOffSupplier leverer true for pause og false for kjøring, aldri null
     * @param pollTimeoutDuration perioden mellom planlagte vurderinger, ikke maksimal tid for en callback
     * @throws IllegalArgumentException hvis perioden er Duration.ZERO eller negativ
     */
    public FeatureToggledKafkaConsumerClient(
            KafkaConsumerClient kafkaConsumerClient,
            Supplier<Boolean> isToggledOffSupplier,
            Duration pollTimeoutDuration) {
        if (pollTimeoutDuration.isNegative() || pollTimeoutDuration.isZero()) {
            throw new IllegalArgumentException("Poll timeout must be positive");
        }
        this.kafkaConsumerClient = kafkaConsumerClient;
        this.isConsumerToggledOffSupplier = isToggledOffSupplier;
        this.pollTimeoutDuration = pollTimeoutDuration;
        lifecycle = new WorkerLifecycle(1, this::closeUnderlyingClient, false);
    }

    /**
     * Vurderer togglen og starter eller stopper den underliggende klienten på kallerens tråd.
     *
     * <p>Bakgrunnsoppgaven er lagt inn, men venter på initialiseringslatchen. Periodiske
     * toggle-vurderinger begynner først etter at klientstatusen er oppdatert og latchen er åpnet.
     * Gjentatt start under aktiv kjøring gjør ingenting og oppretter ingen ny kjøring.
     *
     * <p>Etter stop venter metoden inntil ti sekunder på forrige kjøring før omstart.
     * Start avvises hvis close er satt i gang. Feil fra den første toggle-vurderingen eller
     * underliggende start/stop sendes videre til kalleren etter at løkken har fått stoppsignal.
     * Latchen åpnes også ved feil, slik at bakgrunnsoppgaven kan avsluttes.
     * Ved avbrutt venting gjenopprettes interrupt-statusen før feilen kastes.
     *
     * @throws IllegalStateException hvis wrapperen er lukket, forrige kjøring ikke blir ferdig
     *     innen tidsgrensen, ventingen avbrytes eller kalleren forsøker å vente på eget arbeid
     * @throws RuntimeException hvis toggle-kallet eller underliggende start/stop feiler under første vurdering
     * @throws Error hvis første vurdering eller klientoppdatering kaster Error; feilen sendes videre etter stoppsignal
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CountDownLatch.html#await()">Java 21 venting på initialisering og synlighet mellom tråder</a>
     */
    @Override
    public void start() {
        CountDownLatch initialized = new CountDownLatch(1);
        if (!lifecycle.startWithWaitGuard(
                () -> {
                    if (hasUnsafeWaitCaller()) {
                        throw new IllegalStateException(UNSAFE_WAIT_MESSAGE);
                    }
                },
                () -> toggleLoop(initialized))) {
            return;
        }
        boolean initializedSuccessfully = false;
        try {
            lifecycle.inWorker(this::syncRunningStateWithToggle);
            initializedSuccessfully = true;
        } finally {
            if (!initializedSuccessfully) {
                lifecycle.stop();
            }
            initialized.countDown();
        }
    }

    /**
     * Sender stoppsignal og forsøker å vente inntil ti sekunder på toggle-tråden og underliggende stop.
     *
     * <p>Wrapperen kan startes igjen senere. Kall fra en consumer-callback setter stoppflagget,
     * men får feil i stedet for å blokkere en callback som klienten venter på.
     * Tidsavbrudd og avbrutt venting logges uten å kaste feil. Retur bekrefter ikke fullført stopp.
     *
     * @throws IllegalStateException ved usikker callback-venting, selvventing eller underliggende stoppfeil
     */
    @Override
    public void stop() {
        if (hasUnsafeWaitCaller()) {
            lifecycle.stop();
            throw new IllegalStateException(UNSAFE_WAIT_MESSAGE);
        }
        lifecycle.stopAndAwaitBestEffort(
                WorkerLifecycle.DEFAULT_TIMEOUT, () -> {}, () -> delegateToUnderlying(kafkaConsumerClient::stop));
    }

    /**
     * Setter i gang endelig lukking av wrapperen og venter inntil ti sekunder.
     *
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting, usikker callback-venting eller lukkefeil
     * @see #close(Duration)
     */
    @Override
    public void close() {
        close(WorkerLifecycle.DEFAULT_TIMEOUT);
    }

    /**
     * Venter på toggle-arbeidet og underliggende close uten å avbryte callbacks.
     *
     * <p>Gjentatte kall venter på samme lukking. Metoden virker også før første start.
     * Senere start avvises, også etter tidsavbrudd. Et nytt close-kall kan bekrefte at alt
     * er avsluttet, eller melde tidligere lukkefeil igjen. Ved avbrutt venting gjenopprettes
     * interrupt-statusen før feil kastes. Kall fra en consumer-callback setter i gang lukking,
     * men får ikke vente, slik at klienten og callbacken ikke blir stående og vente på hverandre.
     *
     * @param timeout maksimal samlet ventetid på toggle-arbeidet og underliggende close
     * @throws IllegalArgumentException hvis ventetiden er negativ
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting, usikker callback-venting eller lukkefeil
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#awaitTermination(long,java.util.concurrent.TimeUnit)">Java 21 begrenset avslutningsventing</a>
     */
    public void close(Duration timeout) {
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        if (hasUnsafeWaitCaller()) {
            // En consumer-callback kan være arbeidet toggle-tråden venter på.
            lifecycle.shutdown();
            throw new IllegalStateException(UNSAFE_WAIT_MESSAGE);
        }
        lifecycle.close(timeout);
    }

    /**
     * Bekrefter underliggende lukking i bakgrunnen uten å lagre en utløpt ventefrist som lukkefeil.
     * Bare tidsavbrudd forsøkes igjen; faktiske lukkefeil sendes videre.
     */
    private void closeUnderlyingClient() {
        while (true) {
            try {
                delegateToUnderlying(kafkaConsumerClient::close);
                return;
            } catch (CloseTimeoutException e) {
                log.debug("Kafka consumer is still closing; continuing background wait", e);
            }
        }
    }

    /**
     * Tillater bare direkte delegering til denne wrapperen når kalleren utfører Kafka-arbeid.
     *
     * @return true hvis kalleren er en arbeider uten eksplisitt delegering til denne klienten
     */
    private boolean hasUnsafeWaitCaller() {
        return WorkerLifecycle.isWorkerThread() && DELEGATED_CLIENT.get() != this;
    }

    /**
     * Merker bare den konkrete underliggende klienten som mål for trygg livssyklusdelegering.
     * Tidligere mål gjenopprettes ved nestede kall og feil.
     *
     * @param action start, stopp eller lukking av den underliggende klienten
     */
    private void delegateToUnderlying(Runnable action) {
        KafkaConsumerClient previous = DELEGATED_CLIENT.get();
        DELEGATED_CLIENT.set(kafkaConsumerClient);
        try {
            action.run();
        } finally {
            if (previous == null) {
                DELEGATED_CLIENT.remove();
            } else {
                DELEGATED_CLIENT.set(previous);
            }
        }
    }

    /**
     * Leser underliggende klientstatus, ikke om toggle-arbeidet eller lukkingen er ferdig.
     *
     * @return underliggende klients isRunning-verdi
     */
    @Override
    public boolean isRunning() {
        return kafkaConsumerClient.isRunning();
    }

    /**
     * Venter på initialiseringen og vurderer togglen etter en fast tidsplan.
     *
     * <p>Tidsplanen bruker System.nanoTime, slik at endringer i systemklokken ikke påvirker den.
     * Vurderingene overlapper ikke. Ved forsinkelse kan neste vurdering starte straks.
     * Finally-blokken stopper klienten før kjøringen meldes ferdig. Etter avbrutt venting på
     * initialisering eller pause gjenopprettes interrupt-statusen først når underliggende stop er ferdig.
     *
     * @param initialized latch som åpnes etter startens første toggle-vurdering, også ved oppstartsfeil
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ScheduledExecutorService.html#scheduleAtFixedRate(java.lang.Runnable,long,long,java.util.concurrent.TimeUnit)">Java 21 fixed-rate-semantikk</a>
     */
    private void toggleLoop(CountDownLatch initialized) {
        boolean interrupted = WorkerLifecycle.awaitInitializationAndReportInterruption(initialized);
        if (interrupted) {
            lifecycle.stop();
        }
        try {
            long periodNanos = pollTimeoutDuration.toNanos();
            long nextPoll = System.nanoTime() + periodNanos;
            while (lifecycle.isRunning()) {
                if (lifecycle.pause(Duration.ofNanos(Math.max(0, nextPoll - System.nanoTime())))) {
                    break;
                }
                syncRunningStateWithToggle();
                nextPoll += periodNanos;
            }
        } catch (InterruptedException e) {
            interrupted = true;
        } catch (Exception e) {
            log.error("Failed to synchronize Kafka consumer feature toggle", e);
        } finally {
            // Et ventende interrupt må ikke avbryte underliggende stop før klientens arbeid er ferdig:
            // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CountDownLatch.html#await(long,java.util.concurrent.TimeUnit)
            interrupted |= Thread.interrupted();
            try {
                delegateToUnderlying(kafkaConsumerClient::stop);
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Sammenligner toggle og klientstatus og kaller start eller stop bare når statusen må endres.
     *
     * <p>Stoppflagget sjekkes etter at toggle-verdien er lest. Hvis stop eller close er
     * kalt mens toggle-kallet ventet, endres ikke klientstatusen.
     */
    private void syncRunningStateWithToggle() {
        boolean toggledOff = isConsumerToggledOffSupplier.get();
        // Toggle-kallet kan ha ventet mens stop eller close ble kalt.
        if (!lifecycle.isRunning()) {
            return;
        }
        boolean running = kafkaConsumerClient.isRunning();
        if (toggledOff && running) {
            delegateToUnderlying(kafkaConsumerClient::stop);
        } else if (!toggledOff && !running) {
            delegateToUnderlying(kafkaConsumerClient::start);
        }
    }
}
