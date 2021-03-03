package no.nav.common.kafka.producer;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.Future;

public interface KafkaProducerClient<K, V> {

    Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback);
}
