package no.nav.common.kafka.consumer.feilhandtering;

import no.nav.common.kafka.consumer.ConsumeStatus;

public interface StoredRecordConsumer {

    ConsumeStatus consume(KafkaConsumerRecord<byte[], byte[]> consumerRecord);

}
