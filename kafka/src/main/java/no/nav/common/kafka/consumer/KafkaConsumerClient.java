package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class KafkaConsumerClient<K, V> implements ConsumerRebalanceListener {

    private enum ClientStatus {
        NOT_STARTED, RUNNING, STOPPED
    }

    private final Thread shutdownThread = new Thread(this::stop);

    private final KafkaConsumer<K, V> consumer;

    private final Map<String, TopicConsumer<K, V>> topicConsumers;

    private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor();

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new ConcurrentHashMap<>();

    private final Set<TopicPartition> revokedOrFailedPartitions = ConcurrentHashMap.newKeySet();

    private final ReentrantLock consumptionLock = new ReentrantLock();

    private final AtomicInteger processedRecordCounter = new AtomicInteger();

    private Map<String, ThreadPoolExecutor> topicConsumptionExecutors;

    private volatile ClientStatus clientStatus = ClientStatus.NOT_STARTED;

    public KafkaConsumerClient(KafkaConsumerClientConfig<K, V> config) {
        consumer = new KafkaConsumer<>(config.properties);
        topicConsumers = config.topicListeners;

        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    public void start() {
        if (clientStatus == ClientStatus.RUNNING) {
            return;
        }

        topicConsumptionExecutors = createTopicExecutors(topicConsumers.keySet());

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
            topicConsumptionExecutors.values().forEach(this::drainWaitingTasks);
            pollExecutor.awaitTermination(30, TimeUnit.SECONDS);
            // TODO: Log that everything was finished
        } catch (InterruptedException e) {
            topicConsumptionExecutors.values().forEach(ThreadPoolExecutor::shutdownNow);
            consumer.commitSync(currentOffsets);
            // TODO: Log
        }
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        revokedOrFailedPartitions.addAll(partitions);
        consumer.commitSync(currentOffsets);
        // TODO: Log info
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
       // TODO: Log info
    }

    private void consumeTopics() {
        consumptionLock.unlock();
        consumer.subscribe(new ArrayList<>(topicConsumers.keySet()), this);

        while (true) {
            revokedOrFailedPartitions.clear();
            currentOffsets.clear();
            processedRecordCounter.set(0);

            ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(1000));

            if (records.isEmpty()) {
                continue;
            }

            int totalRecords = records.count();

            for (ConsumerRecord<K, V> record : records) {
                String topic = record.topic();
                TopicConsumer<K, V> topicConsumer = topicConsumers.get(topic);
                ExecutorService executor = topicConsumptionExecutors.get(topic);
                TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

                /*
                    If the client is stopped then fast-track through the remaining records so that
                     we can wait for those that are already being consumed after the while-loop.

                    If some previous record on the same topic + partition has failed to be consumed or has been revoked during rebalancing,
                    then we cannot consume any more records for this topic + partition for the duration of this poll.
                 */
                if (clientStatus == ClientStatus.STOPPED || revokedOrFailedPartitions.contains(topicPartition)) {
                    incrementProcessedRecords(totalRecords);
                    continue;
                }

                executor.submit(() -> {
                    ConsumeStatus status;

                    try {
                        status = topicConsumer.consume(record);
                    } catch (Exception e) {
                        // TODO: Log something here
                        status = ConsumeStatus.FAILED;
                    } finally {
                        incrementProcessedRecords(totalRecords);
                    }

                    if (status == ConsumeStatus.OK) {
                        OffsetAndMetadata offsetAndMetadata = new OffsetAndMetadata(record.offset());
                        currentOffsets.put(topicPartition, offsetAndMetadata);
                    } else {
                        revokedOrFailedPartitions.add(topicPartition);
                    }
                });
            }

            // TODO: All must be wrapped in try catch so that it is guaranteed that the lock will be unlocked

            consumptionLock.lock();

            // TODO: Maybe log something here?
            consumer.commitSync(currentOffsets);
        }
    }

    private void drainWaitingTasks(ThreadPoolExecutor executor) {
        executor.shutdown();

        final BlockingQueue<Runnable> blockingQueue = executor.getQueue();

        processedRecordCounter.addAndGet(blockingQueue.size());

        blockingQueue.clear();
    }

    private void incrementProcessedRecords(int totalRecords) {
        int processedRecords = processedRecordCounter.incrementAndGet();
        if (processedRecords >= totalRecords && consumptionLock.isLocked()) {
            consumptionLock.unlock();
        }
    }

    private static Map<String, ThreadPoolExecutor> createTopicExecutors(Iterable<String> topics) {
        Map<String, ThreadPoolExecutor> executorsMap = new HashMap<>();
        topics.forEach(topic -> executorsMap.put(topic, new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue())));
        return executorsMap;
    }

}
