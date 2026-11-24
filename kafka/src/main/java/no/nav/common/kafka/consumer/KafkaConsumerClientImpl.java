package no.nav.common.kafka.consumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import no.nav.common.kafka.util.WorkerLifecycle;
import org.apache.kafka.clients.consumer.CloseOptions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

/**
 * Kafka-klient med én poll-tråd og én behandlingstråd per partisjon.
 *
 * <p>Consumeren opprettes synkront. Poll-tråden venter på en latch til initialiseringen er ferdig.
 * Under konsumering brukes bare wakeup fra andre tråder. Meldinger behandles i rekkefølge per
 * partisjon. Når behandlingen feiler, utsettes de neste meldingene fra samme partisjon til senere poll.
 * Klienten committer bare fremdrift for meldinger som er behandlet med OK.
 *
 * <p>Stop forsøker å vente på kjøringen og lar klienten startes igjen. Close setter i gang endelig lukking,
 * venter på bakgrunnsarbeidet og frigjør executorene. Etter close kan klienten ikke startes igjen.
 * Vanlig stop logger tidsavbrudd og avbrutt venting. Stop(Duration) og close melder slike feil til kalleren.
 * Callbacks og lukking fortsetter etter tidsavbrudd. Et nytt close-kall kan bekrefte at alt er avsluttet.
 *
 * <p>Metoder som venter, kan ikke kalles fra klientens egen initialisering eller behandling.
 * Da får kalleren en feil i stedet for å vente på arbeidet den selv utfører.
 *
 * @param <K> meldingens nøkkeltype
 * @param <V> meldingens verditype
 * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#multithreaded">Kafka 4.4 trådeierskap og partisjonsrekkefølge</a>
 * @see WorkerLifecycle
 */
@Slf4j
public class KafkaConsumerClientImpl<K, V> implements KafkaConsumerClient, ConsumerRebalanceListener {

    /** Standard tidsgrense for poll, i millisekunder, når konfigurasjonen ikke angir en annen verdi. */
    public static final long DEFAULT_POLL_DURATION_MS = 1000;

    private static final long POLL_ERROR_TIMEOUT_MS = 5000;

    private final WorkerLifecycle lifecycle;
    private final Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new ConcurrentHashMap<>();
    private final Set<TopicPartition> revokedOrFailedPartitions = ConcurrentHashMap.newKeySet();
    private final KafkaConsumerClientConfig<K, V> config;
    private final Supplier<KafkaConsumer<K, V>> consumerFactory;
    private volatile CountDownLatch processedRecordsLatch;
    private volatile KafkaConsumer<K, V> consumer;
    private volatile RuntimeException closeFailure;

    /**
     * Eier partisjonsarbeiderne for én consumer-instans og venter på dem før consumeren lukkes.
     *
     * <p>Interrupt registreres og nullstilles under ventingen. Poll-tråden gjenoppretter statusen
     * etter siste commit og lukking av consumeren. ExecutorService.close brukes ikke, fordi den
     * kan avbryte arbeid hvis ventingen avbrytes.
     *
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#close()">Java 21 close kan kalle shutdownNow ved interrupt</a>
     */
    private static final class PartitionWorkers implements AutoCloseable {
        private final Map<TopicPartition, PartitionWorker> workers = new HashMap<>();
        private boolean interrupted;

        /** Holder én executor som bare den omsluttende ressursen kan bruke og avslutte. */
        private static final class PartitionWorker {
            private final ExecutorService executor = Executors.newSingleThreadExecutor();

            /** Oppretter en executor som behandler én partisjons meldinger i rekkefølge. */
            private PartitionWorker() {}
        }

        /** Oppretter en tom samling som får arbeidere etter hvert som partisjoner behandles. */
        private PartitionWorkers() {}

        /**
         * Legger arbeidet i partisjonens kø og gjenbruker arbeideren mellom pollinger.
         *
         * @param partition partisjonen som bestemmer behandlingsrekkefølgen
         * @param task meldingsbehandling som skal bli ferdig før ressursen lukkes
         */
        private void submit(TopicPartition partition, Runnable task) {
            workers.computeIfAbsent(partition, ignored -> new PartitionWorker())
                    .executor
                    .submit(task);
        }

        /**
         * Avslutter alle køene og venter på arbeid som pågår, uten å sende interrupt til arbeiderne.
         *
         * <p>Ventingen fortsetter etter interrupt og lagrer avbruddet for poll-tråden.
         * Kallerens stop eller close har fortsatt sin egen tidsgrense.
         *
         * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#shutdown()">Java 21 shutdown lar innsendt arbeid bli ferdig</a>
         * @see WorkerLifecycle#awaitTerminationAndReportInterruption(ExecutorService)
         */
        @Override
        public void close() {
            interrupted |= Thread.interrupted();
            workers.values().forEach(worker -> worker.executor.shutdown());
            for (PartitionWorker worker : workers.values()) {
                interrupted |= WorkerLifecycle.awaitTerminationAndReportInterruption(worker.executor);
            }
        }
    }

    /**
     * Oppretter en klient og registrerer endelig lukking som shutdown hook, uten å starte konsumering.
     *
     * @param config Kafka-egenskaper, tidsgrense for poll og topic-consumere
     * @throws IllegalArgumentException hvis topics mangler, poll-tidsgrensen er ugyldig eller auto-commit er aktivert
     */
    public KafkaConsumerClientImpl(KafkaConsumerClientConfig<K, V> config) {
        this(config, true);
    }

    /**
     * Oppretter en klient med valgfri JVM-shutdown-hook, uten å starte konsumering.
     *
     * @param config Kafka-egenskaper, tidsgrense for poll og topic-consumere
     * @param registerShutdownHook true for automatisk lukking; ved false må applikasjonen kalle close
     * @throws IllegalArgumentException hvis konfigurasjonen er ugyldig
     */
    public KafkaConsumerClientImpl(KafkaConsumerClientConfig<K, V> config, boolean registerShutdownHook) {
        this(config, () -> new KafkaConsumer<>(config.properties), registerShutdownHook);
    }

    /**
     * Oppretter en klient med en consumer-fabrikk som kan erstattes i tester.
     *
     * @param config Kafka-egenskaper og topic-consumere
     * @param consumerFactory fabrikk som gir en ny consumer ved oppstart og nytt forsøk etter en avsluttet løkke
     * @throws IllegalArgumentException hvis konfigurasjonen er ugyldig
     */
    KafkaConsumerClientImpl(KafkaConsumerClientConfig<K, V> config, Supplier<KafkaConsumer<K, V>> consumerFactory) {
        this(config, consumerFactory, true);
    }

    KafkaConsumerClientImpl(
            KafkaConsumerClientConfig<K, V> config,
            Supplier<KafkaConsumer<K, V>> consumerFactory,
            boolean registerShutdownHook) {
        validateConfig(config);
        this.config = config;
        this.consumerFactory = consumerFactory;
        lifecycle = new WorkerLifecycle(
                1,
                () -> {
                    if (closeFailure != null) {
                        throw closeFailure;
                    }
                },
                registerShutdownHook,
                this::wakeupConsumer);
    }

    /**
     * Starter én kjøring og initialiserer consumeren synkront før poll-tråden får tilgang.
     *
     * <p>Gjentatt start under aktiv kjøring gjør ingenting. Etter stop venter metoden inntil
     * ti sekunder på at forrige kjøring blir ferdig. Oppstartsfeil fra consumer-fabrikken sendes til kalleren.
     *
     * @throws IllegalStateException hvis close er satt i gang, forrige kjøring ikke blir ferdig,
     *     ventingen avbrytes eller kalleren forsøker å vente på sin egen kjøring
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CountDownLatch.html">Java 21 latch og happens-before ved overføring</a>
     */
    @Override
    public void start() {
        log.info("Starting Kafka consumer client...");
        CountDownLatch initialized = new CountDownLatch(1);
        if (!lifecycle.start(() -> consumeLoop(initialized))) {
            return;
        }
        boolean initializedSuccessfully = false;
        try {
            // Poll-tråden venter til dette forsøket er ferdig, også når initialiseringen feiler.
            lifecycle.inWorker(this::initializeConsumer);
            initializedSuccessfully = true;
        } finally {
            if (!initializedSuccessfully) {
                lifecycle.stop();
            }
            initialized.countDown();
        }
    }

    /**
     * Venter på initialiseringen og starter et nytt consumer-forsøk etter uventede løkkefeil.
     *
     * <p>Forrige consumer og partisjonsarbeidere er lukket før neste forsøk. Et stoppsignal
     * hindrer nye forsøk. Interrupt under initialisering eller pause gjenopprettes.
     *
     * @param initialized latch som åpnes når det synkrone initialiseringsforsøket er ferdig
     * @see WorkerLifecycle#awaitInitializationAndReportInterruption(CountDownLatch)
     */
    private void consumeLoop(CountDownLatch initialized) {
        boolean interrupted = WorkerLifecycle.awaitInitializationAndReportInterruption(initialized);
        if (interrupted) {
            lifecycle.stop();
            Thread.currentThread().interrupt();
        }
        do {
            consumeTopics();
            if (lifecycle.isRunning()) {
                try {
                    if (lifecycle.pause(Duration.ofMillis(POLL_ERROR_TIMEOUT_MS))) {
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } while (lifecycle.isRunning());
    }

    /**
     * Oppretter og abonnerer med en ny consumer før poll-tråden begynner å bruke den.
     *
     * <p>Første kall skjer under initialiseringslatchen. Senere kall skjer på poll-tråden,
     * etter at forrige consumer er lukket. En delvis initialisert consumer lukkes ved oppstartsfeil.
     *
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#multithreaded">Kafka 4.4 trådeierskap</a>
     */
    private void initializeConsumer() {
        consumer = consumerFactory.get();
        closeFailure = null;
        consumer.subscribe(new ArrayList<>(config.topics.keySet()), this);
    }

    /**
     * Sender stoppsignal og forsøker å vente inntil ti sekunder på behandling, commit og lukking.
     *
     * <p>Tidsavbrudd og avbrutt venting logges uten å kaste feil, som tidligere.
     * Interrupt-statusen gjenopprettes. Arbeidet fortsetter i bakgrunnen.
     * Retur fra denne metoden bekrefter derfor ikke at consumeren er ferdig lukket.
     * Bruk stop(Duration) for bekreftet stopp, eller close ved endelig lukking.
     *
     * @throws IllegalStateException ved kall fra klientens eget arbeid eller feil i den stoppede kjøringen
     * @see #stop(Duration)
     */
    @Override
    public void stop() {
        log.info("Stopping Kafka consumer client...");
        lifecycle.stopAndAwaitBestEffort(WorkerLifecycle.DEFAULT_TIMEOUT, this::wakeupConsumer, null);
    }

    /**
     * Setter stoppflagget, vekker Kafka-operasjonen og venter på den kjøringen som stoppes.
     *
     * <p>Når metoden returnerer uten feil, er consumeren og behandlingstrådene avsluttet.
     * Poll-executoren beholdes slik at klienten kan startes igjen. Ved tidsavbrudd fortsetter
     * arbeidet i bakgrunnen. Hvis ventingen avbrytes med interrupt, gjenopprettes interrupt-statusen før feil meldes.
     *
     * @param timeout maksimal ventetid for behandling og lukking; Duration.ZERO venter ikke
     * @throws IllegalArgumentException hvis ventetiden er negativ
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting eller kall fra klientens eget arbeid
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#wakeup()">Kafka 4.4 wakeup ved stopp</a>
     */
    public void stop(Duration timeout) {
        log.info("Awaiting confirmed Kafka consumer stop with timeout {}", timeout);
        lifecycle.stopAndAwait(timeout, this::wakeupConsumer, null);
    }

    /**
     * Vekker en eksisterende consumer uten å sende interrupt til callback- eller databasetråder.
     *
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#multithreaded">Kafka 4.4: bare wakeup er trygt fra en annen tråd</a>
     */
    private void wakeupConsumer() {
        KafkaConsumer<K, V> activeConsumer = consumer;
        if (activeConsumer != null) {
            activeConsumer.wakeup();
        }
    }

    /**
     * Setter i gang endelig lukking og venter inntil ti sekunder, også før første start.
     *
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting, kall fra klientens eget arbeid eller lukkefeil
     * @see #close(Duration)
     */
    @Override
    public void close() {
        close(WorkerLifecycle.DEFAULT_TIMEOUT);
    }

    /**
     * Avviser senere start og venter på lukking av consumeren og avslutning av executorene.
     *
     * <p>Gjentatte kall venter på samme lukking uten å starte den på nytt. Ved tidsavbrudd
     * fortsetter arbeidet og lukkingen i bakgrunnen. En lagret lukkefeil meldes også ved senere kall.
     * Hvis klientens egen kode kaller close, settes lukking i gang, men ventingen avvises.
     * Bruk en annen tråd for å bekrefte at klienten er ferdig lukket.
     *
     * @param timeout maksimal samlet ventetid for klientens arbeid og ressurser
     * @throws IllegalArgumentException hvis ventetiden er negativ
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting, kall fra klientens eget arbeid eller lukkefeil
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#close(org.apache.kafka.clients.consumer.CloseOptions)">Kafka 4.4 consumer-close</a>
     */
    public void close(Duration timeout) {
        lifecycle.close(timeout, this::wakeupConsumer);
    }

    /**
     * Leser om kjøringen er aktivert, ikke om bakgrunnsarbeidet er ferdig.
     *
     * @return true når gjeldende kjøring er aktivert og close ikke er satt i gang
     */
    @Override
    public boolean isRunning() {
        return lifecycle.isRunning();
    }

    /**
     * Markerer hvilke partisjoner Kafka tar tilbake, og forsøker å committe ferdigbehandlet fremdrift.
     *
     * @param partitions partisjoner som Kafka trekker tilbake
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#commitSync(java.util.Map,java.time.Duration)">Kafka 4.4 commit for tildelte partisjoner</a>
     */
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.info("Partitions has been revoked from consumer: {}", Arrays.toString(partitions.toArray()));
        revokedOrFailedPartitions.addAll(partitions);
        try {
            commitCurrentOffsets();
        } catch (Exception e) {
            log.error("Failed to commit offsets when partitions were revoked: {}", offsetsToCommit, e);
        }
    }

    /**
     * Logger den nye partisjonstildelingen uten å endre den lagrede fremdriften.
     *
     * @param partitions partisjoner Kafka har tildelt consumeren
     */
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.info("New partitions has been assigned to the consumer: {}", Arrays.toString(partitions.toArray()));
    }

    /**
     * Utfører poll, seek, commit og close på poll-tråden for én consumer-instans.
     *
     * <p>Meldingene behandles i rekkefølge per partisjon. Løkken venter til batchen er ferdig
     * før commit. Et uventet wakeup behandles som feil. Ved stoppsignal avsluttes kjøringen uten omstart.
     *
     * <p>Try-with-resources venter til behandlingstrådene er ferdige, før siste commit og lukking av consumeren.
     * Kallerens stop eller close har fortsatt begrenset ventetid og melder tidsavbrudd.
     * Bakgrunnsarbeidet fortsetter hvis kalleren må slutte å vente. Interrupt-statusen gjenopprettes etter lukking.
     *
     * <p>Consumeren forsøkes lukket også etter commit-feil. Lukkefeil lagres og meldes ved close.
     *
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#poll(java.time.Duration)">Kafka 4.4 poll og callbacks utenfor poll-fristen</a>
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#close(org.apache.kafka.clients.consumer.CloseOptions)">Kafka 4.4 close kan ikke avbrytes med wakeup</a>
     */
    private void consumeTopics() {
        PartitionWorkers partitionWorkers = new PartitionWorkers();
        boolean interrupted = false;

        try (partitionWorkers) {
            while (lifecycle.isRunning()) {
                if (consumer == null) {
                    initializeConsumer();
                    continue;
                }
                if (!pollAndConsume(partitionWorkers)) {
                    return;
                }
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                interrupted = true;
            }
            log.error("Unexpected exception caught from main loop. Shutting down...", e);
        } finally {
            interrupted |= partitionWorkers.interrupted;
            interrupted |= Thread.interrupted();
            finishConsumption(interrupted);
        }
    }

    /**
     * Poller og behandler én batch, eller tar en avbrytbar pause etter poll-feil.
     *
     * @param partitionWorkers arbeidere som gjenbrukes for denne consumer-instansen
     * @return true når løkken kan fortsette; false når poll eller pausen fikk stoppsignal
     * @throws InterruptedException hvis pausen avbrytes
     * @throws WakeupException ved wakeup uten stoppsignal
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#wakeup()">Kafka 4.4 wakeup fra en annen tråd</a>
     */
    private boolean pollAndConsume(PartitionWorkers partitionWorkers) throws InterruptedException {
        revokedOrFailedPartitions.clear();
        offsetsToCommit.clear();
        ConsumerRecords<K, V> records;
        try {
            records = consumer.poll(Duration.ofMillis(config.pollDurationMs));
        } catch (WakeupException e) {
            if (lifecycle.isRunning()) {
                throw e;
            }
            log.info("Polling was cancelled by wakeup(). Stopping Kafka consumer client...");
            return false;
        } catch (Exception e) {
            log.error("Exception occurred during polling of records. Waiting before trying again", e);
            return !lifecycle.pause(Duration.ofMillis(POLL_ERROR_TIMEOUT_MS));
        }
        if (!records.isEmpty()) {
            consumeBatch(records, partitionWorkers);
        }
        return true;
    }

    /**
     * Legger batchen i partisjonsvise køer og venter på alle meldingene før commit.
     *
     * <p>Fetch-posisjonene settes til batchens første offsets før behandling. Dermed hoppes
     * ikke uferdige meldinger over hvis partisjonen trekkes tilbake eller en melding feiler.
     *
     * @param records batchen som må bli ferdig før neste poll
     * @param partitionWorkers arbeidere med én kø per partisjon
     * @throws InterruptedException hvis batchventingen avbrytes; ressurslukking venter fortsatt på arbeiderne
     * @see #seekToFirstRecordOffsetForEachTopicPartition(ConsumerRecords)
     */
    private void consumeBatch(ConsumerRecords<K, V> records, PartitionWorkers partitionWorkers)
            throws InterruptedException {
        processedRecordsLatch = new CountDownLatch(records.count());
        seekToFirstRecordOffsetForEachTopicPartition(records);
        for (ConsumerRecord<K, V> consumerRecord : records) {
            var topicPartition = new TopicPartition(consumerRecord.topic(), consumerRecord.partition());
            TopicConsumer<K, V> topicConsumer = config.topics.get(consumerRecord.topic());
            partitionWorkers.submit(
                    topicPartition,
                    () -> lifecycle.inWorker(() -> consumeRecord(consumerRecord, topicPartition, topicConsumer)));
        }
        log.debug("ProcessedRecordsLatch.await start");
        var awaitStart = System.currentTimeMillis();
        processedRecordsLatch.await();
        log.debug("ProcessedRecordsLatch.await: finished after {} ms", System.currentTimeMillis() - awaitStart);
        commitBatchOffsets();
    }

    /**
     * Behandler én melding og lagrer neste offset bare ved OK, før batchlatchen telles ned.
     *
     * <p>Et stoppsignal, en tilbakekalt partisjon eller en tidligere feil hopper over kølagt
     * arbeid, men avbryter ikke en callback som allerede kjører.
     *
     * @param consumerRecord meldingen som skal behandles
     * @param topicPartition partisjonen som kan blokkeres for resten av batchen
     * @param topicConsumer callback valgt da meldingen ble lagt i køen
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#commitSync(java.util.Map,java.time.Duration)">Kafka 4.4 commit av neste offset etter ferdigbehandlet melding</a>
     */
    private void consumeRecord(
            ConsumerRecord<K, V> consumerRecord, TopicPartition topicPartition, TopicConsumer<K, V> topicConsumer) {
        try {
            if (!lifecycle.isRunning() || revokedOrFailedPartitions.contains(topicPartition)) {
                return;
            }
            ConsumeStatus status = ConsumerUtils.safeConsume(topicConsumer, consumerRecord);
            if (status == ConsumeStatus.OK) {
                offsetsToCommit.put(topicPartition, new OffsetAndMetadata(consumerRecord.offset() + 1));
            } else {
                revokedOrFailedPartitions.add(topicPartition);
            }
        } catch (Exception e) {
            log.error(
                    "Unexpected error occurred while processing consumer record. topic={} partition={} offset={}",
                    consumerRecord.topic(),
                    consumerRecord.partition(),
                    consumerRecord.offset(),
                    e);
        } finally {
            processedRecordsLatch.countDown();
        }
    }

    /**
     * Forsøker commit av ferdigbehandlet fremdrift og lar pollingen fortsette etter commit-feil.
     *
     * @throws WakeupException ved wakeup uten stoppsignal
     * @see #commitFinalOffsets()
     */
    private void commitBatchOffsets() {
        try {
            commitCurrentOffsets();
        } catch (WakeupException e) {
            if (lifecycle.isRunning()) {
                throw e;
            }
        } catch (Exception e) {
            log.error("Failed to commit offsets: {}", offsetsToCommit, e);
        }
    }

    /**
     * Forsøker siste commit og lukker consumeren etter at partisjonsarbeiderne er ferdige.
     *
     * @param interrupted om ventingen ble avbrutt; statusen gjenopprettes først etter consumer-lukking
     * @see #commitFinalOffsets()
     * @see #closeConsumer()
     */
    private void finishConsumption(boolean interrupted) {
        try {
            if (consumer != null) {
                commitFinalOffsets();
            }
        } catch (Exception e) {
            log.error("Failed to commit offsets while stopping Kafka consumer client", e);
        } finally {
            try {
                closeConsumer();
            } finally {
                consumer = null;
                if (interrupted) {
                    lifecycle.stop();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Lukker consumeren og melder lukkefeil både til den stoppede kjøringen og senere close-kall.
     *
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#close(org.apache.kafka.clients.consumer.CloseOptions)">Kafka 4.4 close kan ikke avbrytes med wakeup og forsøkes også etter commit-feil</a>
     */
    private void closeConsumer() {
        Throwable failure = WorkerLifecycle.runCapturingFailure(() -> {
            if (consumer != null) {
                consumer.close(CloseOptions.timeout(Duration.ofSeconds(3)));
            }
        });
        if (failure != null) {
            closeFailure = new IllegalStateException("Failed to close Kafka consumer", failure);
            lifecycle.stop();
            log.error("Failed to close Kafka consumer", failure);
            if (failure instanceof Error error) {
                throw error;
            }
            throw closeFailure;
        }
    }

    /**
     * Forsøker siste commit etter at alle behandlingstrådene er ferdige.
     *
     * <p>Et wakeup som ikke er håndtert ennå, kan treffe commit i stedet for poll under stopp.
     * Når WakeupException er håndtert, forsøkes commit på nytt med samme offsets.
     * Andre commit-feil håndteres av finally-blokken som lukker consumeren.
     *
     * @throws WakeupException ved wakeup uten et stopp- eller lukkesignal
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#wakeup()">Kafka 4.4 pending wakeup</a>
     */
    private void commitFinalOffsets() {
        while (true) {
            try {
                commitCurrentOffsets();
                return;
            } catch (WakeupException e) {
                if (lifecycle.isRunning()) {
                    throw e;
                }
            }
        }
    }

    /**
     * Flytter fetch-posisjonene og committer fremdrift med inntil tre sekunders ventetid.
     *
     * <p>Offset-kartet tømmes bare etter vellykket commit. Metoden kalles på poll-tråden.
     *
     * @throws WakeupException hvis et wakeup treffer commit
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#commitSync(java.util.Map,java.time.Duration)">Kafka 4.4 synkron commit og neste offset</a>
     */
    private void commitCurrentOffsets() {
        if (!offsetsToCommit.isEmpty()) {
            safeSeek(offsetsToCommit);

            consumer.commitSync(offsetsToCommit, Duration.ofSeconds(3));
            log.info("Offsets committed: {}", offsetsToCommit);
            offsetsToCommit.clear();
        }
    }

    /**
     * Setter hver partisjons fetch-posisjon til batchens første offset for å unngå å hoppe over uferdige meldinger.
     *
     * @param records batchen som skal behandles før neste poll
     * @see #safeSeek(Map)
     */
    private void seekToFirstRecordOffsetForEachTopicPartition(ConsumerRecords<K, V> records) {
        Map<TopicPartition, OffsetAndMetadata> offsetMap = new HashMap<>();

        records.forEach(consumerRecord -> {
            var topicPartition = new TopicPartition(consumerRecord.topic(), consumerRecord.partition());
            var offsetAndMetadata = new OffsetAndMetadata(consumerRecord.offset());

            OffsetAndMetadata storedOffset = offsetMap.get(topicPartition);

            if (storedOffset == null || offsetAndMetadata.offset() < storedOffset.offset()) {
                offsetMap.put(topicPartition, offsetAndMetadata);
            }
        });

        safeSeek(offsetMap);
    }

    /**
     * Flytter fetch-posisjonene på poll-tråden og logger når en partisjon ikke lenger er tildelt.
     *
     * @param offsetMap ønsket fetch-posisjon per partisjon
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#seek(org.apache.kafka.common.TopicPartition,org.apache.kafka.clients.consumer.OffsetAndMetadata)">Kafka 4.4 seek for tildelte partisjoner</a>
     */
    private void safeSeek(Map<TopicPartition, OffsetAndMetadata> offsetMap) {
        offsetMap.forEach((topicPartition, offsetAndMetadata) -> {
            try {
                consumer.seek(topicPartition, offsetAndMetadata);
            } catch (IllegalStateException e) {
                log.warn(
                        "Unable to seek to topicPartition={} offset={}. The TopicPartition is not assigned to this consumer",
                        topicPartition,
                        offsetAndMetadata.offset());
            }
        });
    }

    /**
     * Kontrollerer at klienten har topics, positiv tidsgrense for poll og deaktivert auto-commit.
     *
     * @param config konfigurasjonen som skal valideres før ressurser opprettes
     * @throws IllegalArgumentException hvis en av disse konfigurasjonsverdiene er ugyldig
     */
    private static void validateConfig(KafkaConsumerClientConfig<?, ?> config) {
        if (config.topics.isEmpty()) {
            throw new IllegalArgumentException("\"topics\" must contain at least 1 topic");
        }

        if (config.pollDurationMs <= 0) {
            throw new IllegalArgumentException("\"pollDurationMs\" must be larger than 0");
        }

        if (!Boolean.FALSE.equals(config.properties.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG))) {
            throw new IllegalArgumentException(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG + " must be false!");
        }
    }
}
