package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;

public class KafkaConsumerClient<K, V> implements ConsumerRebalanceListener {

    public final static long DEFAULT_POLL_DURATION_MS = 1000;

    private final static long POLL_ERROR_TIMEOUT_MS = 5000;

    private enum ClientStatus {
        NOT_STARTED, RUNNING, STOPPED
    }

    private final Logger log = LoggerFactory.getLogger(KafkaConsumerClient.class);

    private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor();

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new ConcurrentHashMap<>();

    private final Set<TopicPartition> revokedOrFailedPartitions = ConcurrentHashMap.newKeySet();

    private final ReentrantLock consumptionLock = new ReentrantLock();

    private final AtomicInteger processedRecordCounter = new AtomicInteger();

    private final KafkaConsumerClientConfig<K, V> config;

    private KafkaConsumer<K, V> consumer;

    private volatile ClientStatus clientStatus = ClientStatus.NOT_STARTED;

    public KafkaConsumerClient(KafkaConsumerClientConfig<K, V> config) {
        validateConfig(config);

        this.config = config;

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void start() {
        if (clientStatus == ClientStatus.RUNNING) {
            return;
        }

        clientStatus = ClientStatus.RUNNING;

        log.info("Starting kafka consumer client...");

        pollExecutor.submit(this::consumeTopics);
    }

    public void stop() {
        if (clientStatus == ClientStatus.NOT_STARTED || clientStatus == ClientStatus.STOPPED) {
            return;
        }

        clientStatus = ClientStatus.STOPPED;

        log.info("Stopping kafka consumer client...");

        try {
            pollExecutor.awaitTermination(30, TimeUnit.SECONDS);
            log.info("Client was gracefully shutdown");
        } catch (InterruptedException e) {
            log.error("Failed to stop gracefully,");
            commitCurrentOffsets();
        }
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.info("Partitions has been revoked from consumer: " + Arrays.toString(partitions.toArray()));
        revokedOrFailedPartitions.addAll(partitions);
        commitCurrentOffsets();
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.info("New partitions has been assigned to the consumer: " + Arrays.toString(partitions.toArray()));
    }

    private void consumeTopics() {
        try {
            final List<String> topicNames = new ArrayList<>(config.topics.keySet());
            final Map<String, ExecutorService> topicConsumptionExecutors = createTopicExecutors(topicNames);

            consumer = new KafkaConsumer<>(config.properties);
            consumer.subscribe(topicNames, this);

            consumptionLock.unlock();

            while (clientStatus == ClientStatus.RUNNING) {
                ConsumerRecords<K, V> records;

               try {
                   records = consumer.poll(Duration.ofMillis(config.pollDurationMs));
               } catch (Exception e) {
                   log.error("Exception occurred during polling of records. Waiting before trying again.", e);
                   Thread.sleep(POLL_ERROR_TIMEOUT_MS);
                   continue;
               }

                if (records.isEmpty()) {
                    continue;
                }

                revokedOrFailedPartitions.clear();
                currentOffsets.clear();
                processedRecordCounter.set(0);

                int totalRecords = records.count();

                for (ConsumerRecord<K, V> record : records) {
                    String topic = record.topic();
                    TopicConsumer<K, V> topicConsumer = config.topics.get(topic);
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
                        } catch(Exception e) {
                            String msg = format(
                                    "Unexpected error occurred wile processing record. topic=%s partition=%d offset=%d",
                                    topic, record.partition(), record.offset()
                            );
                            log.error(msg, e);
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

                commitCurrentOffsets();
            }
        } catch (Exception e) {
            log.error("Unexpected exception caught from main loop. Shutting down...", e);
        } finally {
            commitCurrentOffsets();
            consumer.close();
        }
    }

    private void commitCurrentOffsets() {
        if (currentOffsets.size() > 0) {
            consumer.commitSync(currentOffsets);
            log.info("Offsets commited: " + currentOffsets.toString());
            currentOffsets.clear();
        }
    }

    private void incrementProcessedRecords(int totalRecords) {
        int processedRecords = processedRecordCounter.incrementAndGet();
        if (processedRecords >= totalRecords && consumptionLock.isLocked()) {
            consumptionLock.unlock();
        }
    }

    private ConsumeStatus consumeRecord(TopicConsumer<K, V> topicConsumer, ConsumerRecord<K, V> consumerRecord) {
        try {
            return topicConsumer.consume(consumerRecord);
        } catch (Exception e) {
            String msg = format(
                    "Consumer failed to process record from topic=%s partition=%d offset=%d",
                    consumerRecord.topic(),
                    consumerRecord.partition(),
                    consumerRecord.offset()
            );

            log.error(msg, e);
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
