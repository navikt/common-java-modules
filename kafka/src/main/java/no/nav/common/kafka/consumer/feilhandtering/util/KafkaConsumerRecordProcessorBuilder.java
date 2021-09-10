package no.nav.common.kafka.consumer.feilhandtering.util;

import net.javacrumbs.shedlock.core.LockProvider;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessorConfig;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;

import java.time.Duration;
import java.util.List;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.createTopicConsumers;

public class KafkaConsumerRecordProcessorBuilder {

    private final static Duration DEFAULT_MAX_ERROR_BACKOFF = Duration.ofMinutes(10);
    private final static Duration DEFAULT_POLL_TIMEOUT = Duration.ofSeconds(1);
    private final static int DEFAULT_RECORDS_BATCH_SIZE = 100;

    private KafkaConsumerRecordProcessorBuilder() { }

    private LockProvider lockProvider;

    private KafkaConsumerRepository kafkaConsumerRepository;

    private List<TopicConsumerConfig<?, ?>> topicConsumerConfigs;

    private final KafkaConsumerRecordProcessorConfig config = new KafkaConsumerRecordProcessorConfig(
            DEFAULT_MAX_ERROR_BACKOFF,
            DEFAULT_POLL_TIMEOUT,
            DEFAULT_RECORDS_BATCH_SIZE);

    public static KafkaConsumerRecordProcessorBuilder builder() {
        return new KafkaConsumerRecordProcessorBuilder();
    }

    public KafkaConsumerRecordProcessorBuilder withLockProvider(LockProvider lockProvider) {
        this.lockProvider = lockProvider;
        return this;
    }

    public KafkaConsumerRecordProcessorBuilder withKafkaConsumerRepository(KafkaConsumerRepository kafkaConsumerRepository) {
        this.kafkaConsumerRepository = kafkaConsumerRepository;
        return this;
    }

    public KafkaConsumerRecordProcessorBuilder withConsumerConfigs(List<TopicConsumerConfig<?, ?>> topicConsumerConfigs) {
        this.topicConsumerConfigs = topicConsumerConfigs;
        return this;
    }

    public KafkaConsumerRecordProcessorBuilder withPollTimeout(Duration pollTimeout) {
        this.config.setPollTimeout(pollTimeout);
        return this;
    }

    public KafkaConsumerRecordProcessorBuilder withMaxErrorBackoff(Duration maxErrorBackoff) {
        this.config.setMaxErrorBackoff(maxErrorBackoff);
        return this;
    }

    public KafkaConsumerRecordProcessorBuilder withRecordBatchSize(int recordBatchSize) {
        this.config.setRecordBatchSize(recordBatchSize);
        return this;
    }

    public KafkaConsumerRecordProcessor build() {
        if (lockProvider == null) {
            throw new IllegalStateException("Cannot build kafka consumer record processor without lockProvider");
        }

        if (kafkaConsumerRepository == null) {
            throw new IllegalStateException("Cannot build kafka consumer record processor without kafkaConsumerRepository");
        }

        if (topicConsumerConfigs == null) {
            throw new IllegalStateException("Cannot build kafka consumer record processor without recordConsumers");
        }

        if (config.getMaxErrorBackoff().compareTo(config.getPollTimeout()) < 0) {
            throw new IllegalStateException("Cannot build kafka consumer record processor where poll timeout is greater than max error backoff");
        }

        return new KafkaConsumerRecordProcessor(
                lockProvider,
                kafkaConsumerRepository,
                createTopicConsumers(topicConsumerConfigs),
                config
        );
    }
}
