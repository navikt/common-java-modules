package no.nav.common.kafka.consumer.feilhandtering;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.common.kafka.consumer.feilhandtering.backoff.BackoffStrategy;

import java.time.Duration;

@AllArgsConstructor
@Data
public class KafkaConsumerRecordProcessorConfig {
    Duration errorTimeout;
    Duration pollTimeout;
    int recordBatchSize;
    BackoffStrategy backoffStrategy;
}
