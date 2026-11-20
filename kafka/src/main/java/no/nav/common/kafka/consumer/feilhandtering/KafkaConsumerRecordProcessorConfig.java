package no.nav.common.kafka.consumer.feilhandtering;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.common.kafka.consumer.feilhandtering.backoff.BackoffStrategy;

@AllArgsConstructor
@Data
public class KafkaConsumerRecordProcessorConfig {
    Duration errorTimeout;
    Duration pollTimeout;
    int recordBatchSize;
    BackoffStrategy backoffStrategy;
}
