package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;

public class KafkaConsumerClient<K, V> {

    private final Consumer<K, V> consumer;

    // TODO: Kunne vurdert å ha en liste med listeners slik at man kan gjøre flere ting, som f.eks metrikker + feilhandtering
    private final Map<String, KafkaConsumerListener> topicListeners;

    public KafkaConsumerClient(KafkaConsumerClientConfig config) {
        consumer = new KafkaConsumer<>(config.properties);
        topicListeners = config.topicListeners;
        consumer.subscribe(new ArrayList<>(topicListeners.keySet())); // Kunne også hatt 1 consumer pr topic
        startConsumer();
    }

    private void startConsumer() {
        // TODO: Create executor and run consume()
    }

    private void consume() {
        while (true) {
            // Returns empty if duration is exceeded
            ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<K, V> record : records) {
                // Create thread
                //
                System.out.println("Message received: " + record.value());
            }

            consumer.commitSync();
        }
    }

    public void close() {
        consumer.close();
    }

}
