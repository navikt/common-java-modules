package no.nav.common.kafka.producer.feilhandtering.util;

import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor;
import no.nav.common.kafka.producer.feilhandtering.publisher.KafkaProducerRecordPublisher;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository;

import java.util.List;

public class KafkaProducerRecordProcessorBuilder {

    public static final long DEFAULT_ERROR_TIMEOUT_MS = 5000;
    public static final long DEFAULT_POLL_TIMEOUT_MS = 3000;
    public static final long DEFAULT_WAITING_FOR_LEADER_TIMEOUT_MS = 10_000;
    public static final int DEFAULT_RECORDS_BATCH_SIZE = 100;
    public static final boolean DEFAULT_REGISTER_SHUTDOWN_HOOK = true;

    private KafkaProducerRepository producerRepository;
    private KafkaProducerRecordPublisher kafkaProducerRecordPublisher;
    private LeaderElectionClient leaderElectionClient;
    private List<String> topicWhitelist;

    private long errorTimeoutMs = DEFAULT_ERROR_TIMEOUT_MS;
    private long pollTimeoutMs = DEFAULT_POLL_TIMEOUT_MS;
    private long waitingForLeaderTimeoutMs = DEFAULT_WAITING_FOR_LEADER_TIMEOUT_MS;
    private int recordsBatchSize = DEFAULT_RECORDS_BATCH_SIZE;
    private boolean registerShutdownHook = DEFAULT_REGISTER_SHUTDOWN_HOOK;

    private KafkaProducerRecordProcessorBuilder() {
    }

    public static KafkaProducerRecordProcessorBuilder builder() {
        return new KafkaProducerRecordProcessorBuilder();
    }

    public KafkaProducerRecordProcessorBuilder withProducerRepository(KafkaProducerRepository producerRepository) {
        this.producerRepository = producerRepository;
        return this;
    }

    public KafkaProducerRecordProcessorBuilder withRecordPublisher(KafkaProducerRecordPublisher kafkaProducerRecordPublisher) {
        this.kafkaProducerRecordPublisher = kafkaProducerRecordPublisher;
        return this;
    }

    public KafkaProducerRecordProcessorBuilder withLeaderElectionClient(LeaderElectionClient leaderElectionClient) {
        this.leaderElectionClient = leaderElectionClient;
        return this;
    }

    public KafkaProducerRecordProcessorBuilder withTopicWhitelist(List<String> topicWhitelist) {
        this.topicWhitelist = topicWhitelist;
        return this;
    }

    public KafkaProducerRecordProcessorBuilder withErrorTimeoutMs(long errorTimeoutMs) {
        this.errorTimeoutMs = errorTimeoutMs;
        return this;
    }

    public KafkaProducerRecordProcessorBuilder withPollTimeoutMs(long pollTimeoutMs) {
        this.pollTimeoutMs = pollTimeoutMs;
        return this;
    }

    public KafkaProducerRecordProcessorBuilder withWaitingForLeaderTimeoutMs(long waitingForLeaderTimeoutMs) {
        this.waitingForLeaderTimeoutMs = waitingForLeaderTimeoutMs;
        return this;
    }

    public KafkaProducerRecordProcessorBuilder withRecordsBatchSize(int recordsBatchSize) {
        this.recordsBatchSize = recordsBatchSize;
        return this;
    }

    public KafkaProducerRecordProcessorBuilder withShutdownHookEnabled(boolean registerShutdownHook) {
        this.registerShutdownHook = registerShutdownHook;
        return this;
    }

    public KafkaProducerRecordProcessor build() {
        if (producerRepository == null) {
            throw new IllegalStateException("Cannot build kafka producer record processor without producerRepository");
        }

        if (leaderElectionClient == null) {
            throw new IllegalStateException("Cannot build kafka producer record processor without leaderElectionClient");
        }

        if (kafkaProducerRecordPublisher == null) {
            throw new IllegalStateException("Cannot build kafka producer record processor without kafkaProducerRecordPublisher");
        }

        return new KafkaProducerRecordProcessor(
                errorTimeoutMs,
                pollTimeoutMs,
                waitingForLeaderTimeoutMs,
                recordsBatchSize,
                registerShutdownHook,
                producerRepository,
                kafkaProducerRecordPublisher,
                leaderElectionClient,
                topicWhitelist
        );
    }
}
