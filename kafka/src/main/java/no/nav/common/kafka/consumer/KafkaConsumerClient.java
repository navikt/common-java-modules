package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class KafkaConsumerClient<K, V> {

    private final KafkaConsumer<K, V> consumer;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final Map<String, TopicConsumer<K, V>> topicConsumers;

    private final Map<String, ExecutorService> topicConsumptionExecutors;

    public KafkaConsumerClient(KafkaConsumerClientConfig<K, V> config) {
        consumer = new KafkaConsumer<>(config.properties);

        topicConsumers = config.topicListeners;
        topicConsumptionExecutors = createTopicExecutors(config.topicListeners.keySet());

        executorService.submit(this::consumeTopics);
    }

    private void consumeTopics() {
        /**
         * This is a short-hand for {@link #subscribe(Collection, ConsumerRebalanceListener)}, which
         * uses a no-op listener. If you need the ability to seek to particular offsets, you should prefer
         * {@link #subscribe(Collection, ConsumerRebalanceListener)}, since group rebalances will cause partition offsets
         * to be reset. You should also provide your own listener if you are doing your own offset
         * management since the listener gives you an opportunity to commit offsets before a rebalance finishes.
         */
        consumer.subscribe(new ArrayList<>(topicConsumers.keySet()));

        while (true) {
            ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(1000));

            if (records.isEmpty()) {
                continue;
            }

            Map<String, Object> topicRecordStatus;
            AtomicInteger counter = new AtomicInteger();

            for (ConsumerRecord<K, V> record : records) {
                String topic = record.topic();
                TopicConsumer<K, V> topicConsumer = topicConsumers.get(topic);
                ExecutorService executor = topicConsumptionExecutors.get(topic);

                /**
                    Hent ut fra map
                    Hvis siste record på denne topicen har feilet så hopp over
                 */

                executor.submit(() -> {
                    ConsumeStatus status;

                    try {
                        status = topicConsumer.consume(record);
                    } catch (Exception e) {
                        status = ConsumeStatus.FAILED;
                    } finally {
                        counter.incrementAndGet();
                    }

                    // Add to map

                });
            }

            while (counter.get() < records.count()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Commit offset pr topic basert på status i map
        }
    }

    private void consume() {

    }

    public void close() {
        consumer.close();
    }

    private static Map<String, ExecutorService> createTopicExecutors(Iterable<String> topics) {
        Map<String, ExecutorService> executorsMap = new HashMap<>();
        topics.forEach(topic -> executorsMap.put(topic, Executors.newSingleThreadExecutor()));
        return executorsMap;
    }

}
