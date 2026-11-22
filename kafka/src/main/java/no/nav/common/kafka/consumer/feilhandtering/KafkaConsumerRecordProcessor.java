package no.nav.common.kafka.consumer.feilhandtering;

import static java.lang.String.format;
import static no.nav.common.kafka.consumer.util.ConsumerUtils.mapFromStoredRecord;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.util.WorkerLifecycle;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Behandler lagrede Kafka-meldinger på nytt, med ShedLock-låser per partisjon.
 *
 * <p>Én bakgrunnstråd behandler meldinger og sletter dem som er behandlet med OK. En valgfri tråd leser metrikktall fra
 * databasen uten å ta partisjonslåsen. Stop setter stoppflagget uten å vente. Prosessoren kan
 * startes igjen etter stop, men start venter på at forrige kjøring blir ferdig først.
 *
 * <p>Close setter i gang endelig lukking og venter på callbacks, databasesletting og begge
 * executorene. Senere start avvises. Ved tidsavbrudd fortsetter arbeidet i bakgrunnen.
 * Kall fra prosessorens eget arbeid setter i gang lukking, men får feil i stedet for å vente
 * på egen avslutning. Kalleren har fortsatt ansvar for MeterRegistry og repositoryet.
 *
 * @see WorkerLifecycle
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#awaitTermination(long,java.util.concurrent.TimeUnit)">Java 21 fullført arbeid og executor-avslutning</a>
 */
public class KafkaConsumerRecordProcessor implements AutoCloseable {

    private static final String FAILED_MESSAGES_METRIC = "kafka_consumer_failed_or_backedoff_messages_in_batch";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final WorkerLifecycle lifecycle = new WorkerLifecycle(2, () -> {}, true);

    private final LockProvider lockProvider;

    private final KafkaConsumerRepository kafkaConsumerRepository;

    private final Map<String, TopicConsumer<byte[], byte[]>> topicConsumers;

    private final KafkaConsumerRecordProcessorConfig config;

    private final MeterRegistry meterRegistry;

    // Holder sist leste antall meldinger i databasen, per topic og partisjon.
    private final Map<TopicPartition, AtomicLong> failedMessagesGauges = new HashMap<>();

    /**
     * Oppretter prosessoren uten å starte arbeid og registrerer en hook for endelig lukking.
     *
     * @param lockProvider leverandør av partisjonsvise, distribuerte låser
     * @param kafkaRepository lagring for meldinger som har feilet, og behandlingsstatusen deres
     * @param topicConsumers callbacks per topic
     * @param config batchstørrelse, pauser og backoff-strategi
     * @param meterRegistry metrikkregister, eller null for å deaktivere metrikktråden
     */
    public KafkaConsumerRecordProcessor(
            LockProvider lockProvider,
            KafkaConsumerRepository kafkaRepository,
            Map<String, TopicConsumer<byte[], byte[]>> topicConsumers,
            KafkaConsumerRecordProcessorConfig config,
            MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.lockProvider = lockProvider;
        this.kafkaConsumerRepository = kafkaRepository;
        this.config = config;
        this.topicConsumers = topicConsumers;
    }

    /**
     * Starter meldingsbehandling og eventuelt metrikkinnhenting som én kjøring.
     *
     * <p>Gjentatt start under aktiv kjøring gjør ingenting. Omstart venter på forrige kjøring
     * i inntil ti sekunder uten å avbryte en uferdig batch.
     *
     * @throws IllegalStateException hvis close er satt i gang, ventingen overskrider tidsgrensen
     *     eller avbrytes, eller kalleren forsøker å vente på sitt eget arbeid
     */
    public void start() {
        if (meterRegistry == null) {
            lifecycle.start(this::recordHandlerLoop);
        } else {
            lifecycle.start(this::recordHandlerLoop, this::metricsLoop);
        }
    }

    /**
     * Setter stoppflagget uten å vente på callbacks eller databasesletting.
     *
     * <p>En batch som allerede behandles, får bli ferdig. Prosessoren kan startes igjen senere.
     * Bruk close før repositoryet eller andre delte ressurser lukkes.
     *
     * @see #close(Duration)
     */
    public void stop() {
        lifecycle.stop();
    }

    /**
     * Setter i gang endelig lukking og venter inntil ti sekunder på bakgrunnsarbeidet.
     *
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting eller kall fra prosessorens eget arbeid
     * @see #close(Duration)
     */
    @Override
    public void close() {
        lifecycle.close();
    }

    /**
     * Avviser senere start og venter på callbacks, sletting og begge executorene.
     *
     * <p>Gjentatte kall venter på samme lukking. Metoden virker også før første start.
     * Ved tidsavbrudd fortsetter arbeidet i bakgrunnen. Et senere close-kall kan bekrefte at
     * alt er avsluttet. Ved avbrutt venting gjenopprettes interrupt-statusen før feil meldes.
     * Kall fra prosessorens eget arbeid setter i gang lukking, men får ikke vente på egen avslutning.
     *
     * @param timeout maksimal ventetid for avslutningen
     * @throws IllegalArgumentException hvis ventetiden er negativ
     * @throws IllegalStateException ved tidsavbrudd, avbrutt venting eller kall fra prosessorens eget arbeid
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#shutdown()">Java 21 ordnet shutdown uten å avbryte batchen</a>
     */
    public void close(Duration timeout) {
        lifecycle.close(timeout);
    }

    /**
     * Henter partisjoner og behandler lagrede batcher til kjøringens stoppflagg er satt.
     *
     * <p>Ved behandlingsfeil logges feilen, og løkken tar en pause før neste forsøk.
     * Hvis pausen avbrytes med interrupt, avsluttes løkken og interrupt-statusen gjenopprettes.
     * Close sender ikke interrupt til callbacks.
     */
    private void recordHandlerLoop() {
        List<String> topics = new ArrayList<>(topicConsumers.keySet());

        try {
            while (lifecycle.isRunning()) {
                try {
                    List<TopicPartition> uniquePartitions = kafkaConsumerRepository.getTopicPartitions(topics);

                    consumeFromTopicPartitions(uniquePartitions);

                    if (lifecycle.pause(config.pollTimeout)) {
                        return;
                    }
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Failed to consume stored Kafka records", e);
                    if (lifecycle.pause(config.errorTimeout)) {
                        return;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Unexpected exception caught in stored consumer record handler loop", e);
        } finally {
            log.info("Closing Kafka consumer record processor...");
        }
    }

    /**
     * Oppdaterer gauges med antall meldinger fra databasen og nullstiller partisjoner uten slike meldinger.
     *
     * <p>Løkken kjører på en egen executor uten ShedLock, men følger kjøringens stoppflagg.
     * Close venter også på en pågående metrikkspørring. Hvis ventingen avbrytes,
     * gjenopprettes interrupt-statusen før tråden avsluttes.
     */
    void metricsLoop() {
        List<String> topics = new ArrayList<>(topicConsumers.keySet());
        while (lifecycle.isRunning()) {
            try {
                updateFailedMessagesMetrics(topics);
                if (lifecycle.pause(config.pollTimeout)) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Failed to update failed-or-backedoff metrics", e);
                try {
                    if (lifecycle.pause(config.errorTimeout)) {
                        return;
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Leser antall feilede meldinger og nullstiller kjente partisjoner som ikke lenger har slike meldinger.
     *
     * @param topics topics som metrikkspørringen skal omfatte
     */
    private void updateFailedMessagesMetrics(List<String> topics) {
        Map<TopicPartition, Long> counts = kafkaConsumerRepository.getFailedRecordCounts(topics);
        counts.forEach(this::setFailedMessagesGauge);
        failedMessagesGauges.forEach((tp, holder) -> {
            if (!counts.containsKey(tp)) {
                holder.set(0L);
            }
        });
    }

    /**
     * Registrerer eller oppdaterer antallet meldinger som har feilet, for én partisjon.
     *
     * @param topicPartition partisjonen metrikkverdien tilhører
     * @param value gjeldende antall fra databasen
     */
    private void setFailedMessagesGauge(TopicPartition topicPartition, Long value) {
        AtomicLong holder = failedMessagesGauges.computeIfAbsent(topicPartition, tp -> {
            AtomicLong newHolder = new AtomicLong();
            Gauge.builder(FAILED_MESSAGES_METRIC, newHolder, AtomicLong::get)
                    .tag("topic", tp.topic())
                    .tag("partition", String.valueOf(tp.partition()))
                    .strongReference(true)
                    .register(meterRegistry);
            return newHolder;
        });
        holder.set(value);
    }

    /**
     * Låser og behandler en batch per partisjon, med backoff og blokkering av nøkler som har feilet.
     *
     * <p>Meldinger som er behandlet med OK, slettes før låsen frigjøres. Stop hindrer nye
     * partisjonsbatcher, men avbryter ikke behandlingen eller slettingen av en påbegynt batch.
     *
     * @param uniquePartitions partisjoner med lagrede records som skal vurderes
     */
    private void consumeFromTopicPartitions(List<TopicPartition> uniquePartitions) {
        uniquePartitions.forEach(this::consumeFromTopicPartition);
    }

    /**
     * Låser én partisjon og fullfører batchen før låsen frigjøres, også etter stoppsignal.
     *
     * @param topicPartition partisjonen som skal vurderes dersom kjøringen fortsatt er aktiv
     */
    private void consumeFromTopicPartition(TopicPartition topicPartition) {
        if (!lifecycle.isRunning()) {
            return;
        }
        Optional<SimpleLock> lock = Optional.empty();
        try {
            lock = acquireLock(topicPartition);
            if (lock.isEmpty()) {
                return;
            }
            consumePartitionBatch(topicPartition);
        } catch (Exception e) {
            log.error("Unexpected exception caught while processing stored consumer records", e);
        } finally {
            lock.ifPresent(SimpleLock::unlock);
        }
    }

    /**
     * Behandler en låst partisjons batch i rekkefølge og sletter ferdigbehandlede meldinger samlet.
     *
     * @param topicPartition partisjonen kalleren holder låsen for
     */
    private void consumePartitionBatch(TopicPartition topicPartition) {
        List<StoredConsumerRecord> records = kafkaConsumerRepository.getRecords(
                topicPartition.topic(), topicPartition.partition(), config.recordBatchSize);
        TopicConsumer<byte[], byte[]> recordConsumer = topicConsumers.get(topicPartition.topic());
        List<Long> recordsToDelete = new ArrayList<>();
        Set<Bytes> failedOrBackedOffKeys = new HashSet<>();
        records.forEach(storedRecord -> {
            if (consumeStoredRecord(storedRecord, recordConsumer, failedOrBackedOffKeys)) {
                recordsToDelete.add(storedRecord.getId());
            }
        });
        if (!recordsToDelete.isEmpty()) {
            kafkaConsumerRepository.deleteRecords(recordsToDelete);
            log.info("Stored consumer records deleted {}", Arrays.toString(recordsToDelete.toArray()));
        }
    }

    /**
     * Vurderer backoff, behandler én melding og blokkerer senere meldinger med samme nøkkel ved feil.
     *
     * @param storedRecord meldingen som vurderes i batchens rekkefølge
     * @param recordConsumer callback for partisjonens topic
     * @param failedOrBackedOffKeys nøkler som skal hoppes over resten av batchen; null-nøkler blokkeres ikke
     * @return true bare når meldingen er behandlet med OK og kan slettes
     */
    private boolean consumeStoredRecord(
            StoredConsumerRecord storedRecord,
            TopicConsumer<byte[], byte[]> recordConsumer,
            Set<Bytes> failedOrBackedOffKeys) {
        Bytes keyBytes = Bytes.wrap(storedRecord.getKey());
        if (failedOrBackedOffKeys.contains(keyBytes)) {
            return false;
        }
        if (shouldBackoff(storedRecord)) {
            if (keyBytes != null) {
                failedOrBackedOffKeys.add(keyBytes);
            }
            return false;
        }
        ConsumeStatus status;
        Exception exception = null;
        try {
            status = recordConsumer.consume(mapFromStoredRecord(storedRecord));
        } catch (Exception e) {
            exception = e;
            status = ConsumeStatus.FAILED;
        }
        if (status == ConsumeStatus.OK) {
            log.info(
                    "Successfully processed stored record topic={} partition={} offset={} dbId={}",
                    storedRecord.getTopic(),
                    storedRecord.getPartition(),
                    storedRecord.getOffset(),
                    storedRecord.getId());
            return true;
        }
        String message = format(
                "Failed to process stored consumer record topic=%s partition=%d offset=%d dbId=%d",
                storedRecord.getTopic(), storedRecord.getPartition(), storedRecord.getOffset(), storedRecord.getId());
        log.error(message, exception);
        kafkaConsumerRepository.incrementRetries(storedRecord.getId());
        if (keyBytes != null) {
            failedOrBackedOffKeys.add(keyBytes);
        }
        return false;
    }

    /**
     * Vurderer om meldingens neste tillatte forsøk ligger etter nåværende tidspunkt.
     *
     * @param storedRecord meldingen med forsøkshistorikk
     * @return true hvis backoff-perioden ikke er utløpt
     */
    private boolean shouldBackoff(StoredConsumerRecord storedRecord) {
        Instant now = Instant.now();
        Instant nextAttempt =
                getLastAttempt(storedRecord).plus(config.backoffStrategy.getBackoffDuration(storedRecord));
        return now.isBefore(nextAttempt);
    }

    /**
     * Leser tidspunktet for siste behandlingsforsøk, eller meldingens timestamp før første forsøk.
     *
     * @param storedRecord meldingen med tidspunktet for et eventuelt tidligere forsøk
     * @return tidspunktet som backoff-perioden regnes fra
     */
    private Instant getLastAttempt(StoredConsumerRecord storedRecord) {
        if (storedRecord.getLastRetry() != null) {
            return storedRecord.getLastRetry().toInstant();
        } else {
            return Instant.ofEpochMilli(storedRecord.getTimestamp());
        }
    }

    /**
     * Forsøker å ta en partisjonslås med maksimalt fem minutters og minst fem sekunders låsetid.
     *
     * @param topicPartition partisjonen som skal låses
     * @return låsen som må frigjøres etter behandling, eller tom hvis den ikke kunne tas
     */
    private Optional<SimpleLock> acquireLock(TopicPartition topicPartition) {
        String name = "kcrp-" + topicPartition.topic() + "-" + topicPartition.partition();
        LockConfiguration configuration =
                new LockConfiguration(Instant.now(), name, Duration.ofMinutes(5), Duration.ofSeconds(5));
        return lockProvider.lock(configuration);
    }
}
