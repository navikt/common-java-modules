package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class KafkaConsumerClient<K, V> implements ConsumerRebalanceListener {

    public final static long DEFAULT_POLL_DURATION_MS = 1000;

    private enum ClientStatus {
        NOT_STARTED, RUNNING, STOPPED
    }

    private final long pollDurationMs;

    private final KafkaConsumer<K, V> consumer;

    private final Map<String, TopicConsumer<K, V>> topics;

    private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor();

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new ConcurrentHashMap<>();

    private final Set<TopicPartition> revokedOrFailedPartitions = ConcurrentHashMap.newKeySet();

    private final ReentrantLock consumptionLock = new ReentrantLock();

    private final AtomicInteger processedRecordCounter = new AtomicInteger();

    private Map<String, ExecutorService> topicConsumptionExecutors;

    private volatile ClientStatus clientStatus = ClientStatus.NOT_STARTED;

    public KafkaConsumerClient(KafkaConsumerClientConfig<K, V> config) {
        validateConfig(config);

        pollDurationMs = config.pollDurationMs;
        consumer = new KafkaConsumer<>(config.properties);
        topics = config.topics;

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void start() {
        if (clientStatus == ClientStatus.RUNNING) {
            return;
        }

        topicConsumptionExecutors = createTopicExecutors(topics.keySet());

        clientStatus = ClientStatus.RUNNING;

        pollExecutor.submit(this::consumeTopics);

        // TODO: Log
    }

    public void stop() {
        if (clientStatus == ClientStatus.NOT_STARTED || clientStatus == ClientStatus.STOPPED) {
            return;
        }

        clientStatus = ClientStatus.STOPPED;

        // TODO: Log

        try {
            pollExecutor.awaitTermination(30, TimeUnit.SECONDS);
            // TODO: Log that everything was finished
        } catch (InterruptedException e) {
            commitCurrentOffsets();
            // TODO: Log
        }
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        revokedOrFailedPartitions.addAll(partitions);
        commitCurrentOffsets();
        // TODO: Log info
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // TODO: Log info
    }

    private void consumeTopics() {
        try {
            consumptionLock.unlock();
            consumer.subscribe(new ArrayList<>(topics.keySet()), this);

            while (clientStatus == ClientStatus.RUNNING) {
                revokedOrFailedPartitions.clear();
                currentOffsets.clear();
                processedRecordCounter.set(0);

                // TODO: Wrap this with try-catch so that the main loop does not stop?
                ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(pollDurationMs));

                if (records.isEmpty()) {
                    continue;
                }

                int totalRecords = records.count();

                for (ConsumerRecord<K, V> record : records) {
                    String topic = record.topic();
                    TopicConsumer<K, V> topicConsumer = topics.get(topic);
                    ExecutorService executor = topicConsumptionExecutors.get(topic);
                    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

                    executor.submit(() -> {
                        try {
                            /*
                                If the client is stopped then fast-track through the remaining records so that
                                 we can wait for those that are already being consumed after the while-loop.

                                If some previous record on the same topic + partition has failed to be consumed or has been revoked during rebalancing,
                                 then we cannot consume any more records for this topic + partition for the duration of this poll.

                                 The finally-clause will make sure that the processed records counter is incremented.
                            */
                            if (clientStatus == ClientStatus.STOPPED || revokedOrFailedPartitions.contains(topicPartition)) {
                                return;
                            }

                            ConsumeStatus status = consumeRecord(topicConsumer, record);

                            if (status == ConsumeStatus.OK) {
                                OffsetAndMetadata offsetAndMetadata = new OffsetAndMetadata(record.offset());
                                currentOffsets.put(topicPartition, offsetAndMetadata);
                            } else {
                                revokedOrFailedPartitions.add(topicPartition);
                            }
                        } finally {
                            incrementProcessedRecords(totalRecords);
                        }
                    });
                }

                /*
                    If there are unprocessed records then we wait until all are processed.
                    Since the records are consumed async this should always happen,
                     but we still check so we dont get stuck in a deadlock.
                */
                if (processedRecordCounter.get() < totalRecords) {
                    consumptionLock.lock();
                }

                // TODO: Maybe log something here?

                commitCurrentOffsets();
            }
        } catch (Exception e) {
            // TODO: Log
        } finally {
            consumer.close();
        }
    }

    private void commitCurrentOffsets() {
        if (currentOffsets.size() > 0) {
            consumer.commitSync(currentOffsets);
            currentOffsets.clear();
        }
    }

    private void incrementProcessedRecords(int totalRecords) {
        int processedRecords = processedRecordCounter.incrementAndGet();
        if (processedRecords >= totalRecords && consumptionLock.isLocked()) {
            consumptionLock.unlock();
        }
    }

    private ConsumeStatus consumeRecord(TopicConsumer<K, V> consumer, ConsumerRecord<K, V> record) {
        try {
            return consumer.consume(record);
        } catch (Exception e) {
            // TODO: Log something here
            return ConsumeStatus.FAILED;
        }
    }

    private static void validateConfig(KafkaConsumerClientConfig<?, ?> config) {
        if (config.topics.isEmpty()) {
            throw new IllegalArgumentException("\"topics\" must contain at least 1 topic");
        }

        if (config.pollDurationMs <= 0) {
            throw new IllegalArgumentException("\"pollDurationMs\" must be larger than 0");
        }
    }

    private static Map<String, ExecutorService> createTopicExecutors(Iterable<String> topics) {
        Map<String, ExecutorService> executorsMap = new HashMap<>();
        topics.forEach(topic -> executorsMap.put(topic, Executors.newSingleThreadExecutor()));
        return executorsMap;
    }

}
