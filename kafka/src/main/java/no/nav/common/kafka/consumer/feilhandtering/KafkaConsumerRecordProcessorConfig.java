package no.nav.common.kafka.consumer.feilhandtering;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;

@AllArgsConstructor
@Data
public class KafkaConsumerRecordProcessorConfig {
    Duration errorTimeout;
    Duration pollTimeout;
    int recordBatchSize;
}
