package no.nav.common.kafka.consumer.feilhandtering.backoff;

import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor;
import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord;

import java.time.Duration;

/**
 * Decides how long {@link KafkaConsumerRecordProcessor} should back off when retrying consumption of stored kafka records.
 */
public interface BackoffStrategy {

    Duration getBackoffDuration(StoredConsumerRecord record);

}
