package no.nav.common.kafka.producer.feilhandtering;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.feilhandtering.publisher.BatchedKafkaProducerRecordPublisher;
import no.nav.common.kafka.producer.feilhandtering.publisher.KafkaProducerRecordPublisher;
import no.nav.common.kafka.producer.feilhandtering.util.KafkaProducerRecordProcessorBuilder;
import no.nav.common.kafka.util.WorkerLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publiserer lagrede Kafka-meldinger batchvis når instansen er valgt som leder.
 *
 * <p>Batchene behandles etter hverandre. Publisheren rapporterer hvilke meldinger som kan slettes.
 * Prosessoren lukker publisheren, ikke produceren direkte. Prosessoren venter på publisering og
 * tilhørende databasesletting før publisheren lukkes én gang. CloseAndAwait venter til publisheren
 * er lukket og executoren er avsluttet, også før første start. Den eldre close-metoden ber om
 * lukking uten å vente. Begge avviser senere start.
 *
 * <p>Ved tidsavbrudd fortsetter arbeidet i bakgrunnen. Hvis ventingen avbrytes,
 * gjenopprettes interrupt-statusen og en feil meldes. Kall fra prosessorens eget arbeid setter
 * i gang lukking, men får ikke vente på egen avslutning. Lukkefeil fra publisheren lagres
 * og meldes ved closeAndAwait, også ved gjentatte kall.
 *
 * @see KafkaProducerRecordPublisher
 * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html#close(java.time.Duration)">Kafka 4.4 close-frist og callback-begrensning</a>
 */
public class KafkaProducerRecordProcessor implements AutoCloseable {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final long errorTimeoutMs;
    private final long pollTimeoutMs;
    private final long waitingForLeaderTimeoutMs;
    private final int recordsBatchSize;
    private final KafkaProducerRepository producerRepository;
    private final KafkaProducerRecordPublisher kafkaProducerRecordPublisher;
    private final LeaderElectionClient leaderElectionClient;
    // If the list is not null then it will be used to filter which records will be sent to Kafka
    private final List<String> topicWhitelist;

    private final WorkerLifecycle lifecycle;

    /**
     * Oppretter prosessoren uten å starte arbeid. Kalleren har fortsatt ansvar for repositoryet.
     *
     * @param errorTimeoutMs pause etter prosesseringsfeil, i millisekunder
     * @param pollTimeoutMs pause etter en mindre enn full batch, i millisekunder
     * @param waitingForLeaderTimeoutMs pause mens instansen ikke er leder, i millisekunder
     * @param recordsBatchSize maksimal batchstørrelse
     * @param registerShutdownHook om JVM-avslutning skal be om endelig lukking
     * @param producerRepository repository med meldinger som leses og slettes etter vellykket publisering
     * @param kafkaProducerRecordPublisher publisher som prosessoren lukker én gang; publisheren lukker produceren
     * @param leaderElectionClient avgjør om instansen får publisere
     * @param topicWhitelist tillatte topics, eller null for alle topics
     */
    public KafkaProducerRecordProcessor(
            long errorTimeoutMs,
            long pollTimeoutMs,
            long waitingForLeaderTimeoutMs,
            int recordsBatchSize,
            boolean registerShutdownHook,
            KafkaProducerRepository producerRepository,
            KafkaProducerRecordPublisher kafkaProducerRecordPublisher,
            LeaderElectionClient leaderElectionClient,
            List<String> topicWhitelist) {
        this.errorTimeoutMs = errorTimeoutMs;
        this.pollTimeoutMs = pollTimeoutMs;
        this.waitingForLeaderTimeoutMs = waitingForLeaderTimeoutMs;
        this.recordsBatchSize = recordsBatchSize;
        this.producerRepository = producerRepository;
        this.kafkaProducerRecordPublisher = kafkaProducerRecordPublisher;
        this.leaderElectionClient = leaderElectionClient;
        this.topicWhitelist = topicWhitelist;

        lifecycle = new WorkerLifecycle(
                1,
                () -> {
                    try {
                        kafkaProducerRecordPublisher.close();
                    } catch (IOException e) {
                        log.error("Failed to close Kafka producer record publisher", e);
                        throw new UncheckedIOException(e);
                    }
                },
                registerShutdownHook);
    }

    /**
     * Oppretter prosessoren med standardinnstillinger og en batched publisher som lukker produceren.
     *
     * @param producerRepository lagring for records som skal publiseres
     * @param producerClient producer-klienten som publisheren bruker og lukker
     * @param leaderElectionClient avgjør om instansen får publisere
     * @param topicWhitelist tillatte topics, eller null for alle
     * @see BatchedKafkaProducerRecordPublisher
     */
    public KafkaProducerRecordProcessor(
            KafkaProducerRepository producerRepository,
            KafkaProducerClient<byte[], byte[]> producerClient,
            LeaderElectionClient leaderElectionClient,
            List<String> topicWhitelist) {
        this(
                KafkaProducerRecordProcessorBuilder.DEFAULT_ERROR_TIMEOUT_MS,
                KafkaProducerRecordProcessorBuilder.DEFAULT_POLL_TIMEOUT_MS,
                KafkaProducerRecordProcessorBuilder.DEFAULT_WAITING_FOR_LEADER_TIMEOUT_MS,
                KafkaProducerRecordProcessorBuilder.DEFAULT_RECORDS_BATCH_SIZE,
                KafkaProducerRecordProcessorBuilder.DEFAULT_REGISTER_SHUTDOWN_HOOK,
                producerRepository,
                new BatchedKafkaProducerRecordPublisher(producerClient),
                leaderElectionClient,
                topicWhitelist);
    }

    /**
     * Oppretter prosessoren med standardinnstillinger, batched publisher og uten topic-filter.
     *
     * @param producerRepository lagring for records som skal publiseres
     * @param producerClient producer-klienten som publisheren bruker og lukker
     * @param leaderElectionClient avgjør om instansen får publisere
     */
    public KafkaProducerRecordProcessor(
            KafkaProducerRepository producerRepository,
            KafkaProducerClient<byte[], byte[]> producerClient,
            LeaderElectionClient leaderElectionClient) {
        this(producerRepository, producerClient, leaderElectionClient, null);
    }

    /**
     * Starter én bakgrunnstråd for publisering. Gjentatt start under aktiv kjøring gjør ingenting.
     *
     * @throws IllegalStateException hvis close allerede er satt i gang
     */
    public void start() {
        lifecycle.start(this::recordHandlerLoop);
    }

    /**
     * Setter i gang endelig lukking uten å vente på at arbeidet og ressurslukkingen blir ferdig.
     *
     * <p>Metoden beholder ikke-blokkerende oppførsel for eksisterende klienter.
     * Publisering og databasesletting som pågår, fortsetter før publisheren lukkes.
     * Gjentatte kall starter ikke lukkingen på nytt. Metoden kan også kalles fra eget arbeid.
     *
     * @deprecated Venter ikke på pågående publisering, databasesletting eller ressurslukking.
     *     Bruk {@link #closeAndAwait()} eller {@link #closeAndAwait(Duration)} for bekreftet avslutning.
     *     Avhengigheter som databasen må holdes åpne til avslutningen er bekreftet.
     *     Denne metoden er merket for fjerning.
     */
    @Deprecated(forRemoval = true)
    @Override
    public void close() {
        log.info("Closing Kafka producer record processor...");
        lifecycle.shutdown();
    }

    /**
     * Setter i gang endelig lukking og venter inntil ti sekunder, også før første start.
     *
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting, kall fra prosessorens eget arbeid eller lukkefeil
     * @see #closeAndAwait(Duration)
     */
    public void closeAndAwait() {
        closeAndAwait(WorkerLifecycle.DEFAULT_TIMEOUT);
    }

    /**
     * Avviser senere start og venter til publisering, databasesletting og lukking av ressursene er ferdig.
     *
     * <p>Publisheren lukkes én gang, også uten start. Gjentatte kall venter på samme lukking
     * og melder lagrede lukkefeil. Ved tidsavbrudd fortsetter arbeidet i bakgrunnen.
     * Ved avbrutt venting gjenopprettes interrupt-statusen før feil kastes.
     * Kall fra prosessorens eget arbeid setter i gang lukking, men får ikke vente på egen avslutning.
     * Metoden må ikke kalles fra producerens send-callback, fordi den venter på at callbacken blir ferdig.
     *
     * @param timeout maksimal samlet ventetid
     * @throws IllegalArgumentException hvis ventetiden er negativ
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting, kall fra prosessorens eget arbeid eller lukkefeil
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html#send(org.apache.kafka.clients.producer.ProducerRecord,org.apache.kafka.clients.producer.Callback)">Kafka 4.4 callbacks på producerens I/O-tråd</a>
     */
    public void closeAndAwait(Duration timeout) {
        lifecycle.close(timeout);
    }

    /**
     * Leser og publiserer batcher mens instansen er leder, frem til kjøringen stoppes.
     *
     * <p>Ved feil logges feilen, og løkken tar en pause. Lukking vekker pausene uten å avbryte
     * publisering eller sletting. Finally-blokken starter endelig lukking, også når løkken feiler.
     */
    private void recordHandlerLoop() {
        try {
            while (lifecycle.isRunning()) {
                if (!processNextBatch()) {
                    return;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Unexpected exception caught in producer record handler loop", e);
        } finally {
            lifecycle.shutdown();
        }
    }

    /**
     * Kontrollerer lederstatus og behandler én batch med samme pauser ved lederskap, lite arbeid og feil.
     *
     * @return true når løkken kan fortsette; false når en pause fikk stoppsignal
     * @throws InterruptedException hvis en pause avbrytes
     */
    private boolean processNextBatch() throws InterruptedException {
        try {
            if (!leaderElectionClient.isLeader()) {
                return !lifecycle.pause(Duration.ofMillis(waitingForLeaderTimeoutMs));
            }
            List<StoredProducerRecord> records = topicWhitelist == null
                    ? producerRepository.getRecords(recordsBatchSize)
                    : producerRepository.getRecords(recordsBatchSize, topicWhitelist);
            if (!records.isEmpty()) {
                publishStoredRecords(records);
            }
            if (records.size() < recordsBatchSize) {
                return !lifecycle.pause(Duration.ofMillis(pollTimeoutMs));
            }
            return true;
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process Kafka producer records", e);
            return !lifecycle.pause(Duration.ofMillis(errorTimeoutMs));
        }
    }

    /**
     * Venter på publisheren og sletter bare meldingene den rapporterer som publisert.
     *
     * <p>Batched-publisheren sender asynkront og bruker flush for å vente på sends og callbacks.
     * Queued-publisheren sender synkront. Producer-callbacks må ikke kalle prosessorens closeAndAwait,
     * siden metoden venter på at publiseringen og callbackene blir ferdige.
     *
     * @param records batchen som skal publiseres før eventuell sletting
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html#flush()">Kafka 4.4 flush venter på tidligere sends og callbacks</a>
     * @see <a href="https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html#send(org.apache.kafka.clients.producer.ProducerRecord,org.apache.kafka.clients.producer.Callback)">Kafka 4.4 tråder som kjører producer-callbacks</a>
     */
    private void publishStoredRecords(List<StoredProducerRecord> records) {
        var idsToDelete = kafkaProducerRecordPublisher.publishStoredRecords(records);
        producerRepository.deleteRecords(idsToDelete);
    }
}
