package no.nav.common.kafka.producer;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.KafkaConsumerClientConfig;
import no.nav.common.kafka.consumer.KafkaConsumerClientImpl;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static no.nav.common.kafka.utils.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class KafkaProducerClientImplIntegrationTest {

    private final static String TEST_TOPIC = "test-topic";

    private KafkaProducerClientImpl<String, String> producerClient;

    @ClassRule
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));

    @Before
    public void setup() {
        kafka.start();
        String brokerUrl = kafka.getBootstrapServers();

        producerClient = new KafkaProducerClientImpl<>(kafkaTestProducerProperties(brokerUrl));

        AdminClient admin = KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl));
        admin.deleteTopics(List.of(TEST_TOPIC));
        admin.createTopics(List.of(new NewTopic(TEST_TOPIC, 1, (short) 1)));
        admin.close(); // Apply changes
    }

    @After
    public void cleanup() {
        producerClient.close();
        kafka.stop();
    }

    @Test
    public void should_produce_record() throws InterruptedException {
        AtomicReference<ConsumerRecord<String, String>> recordRef = new AtomicReference<>();

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(TEST_TOPIC, (record) -> {
                    recordRef.set(record);
                    return ConsumeStatus.OK;
                })
        ));

        producerClient.send(new ProducerRecord<>(TEST_TOPIC, "key", "value"));

        consumerClient.start();
        Thread.sleep(1500);
        consumerClient.stop();

        ConsumerRecord<String, String> record = recordRef.get();
        assertEquals(TEST_TOPIC, record.topic());
        assertEquals("key", record.key());
        assertEquals("value", record.value());
    }

    @Test
    public void should_produce_multiple_records() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(TEST_TOPIC, (record) -> {
                    counter.incrementAndGet();
                    return ConsumeStatus.OK;
                })
        ));

        producerClient.send(new ProducerRecord<>(TEST_TOPIC, "key", "value"));
        producerClient.send(new ProducerRecord<>(TEST_TOPIC, "key", "value"));
        producerClient.send(new ProducerRecord<>(TEST_TOPIC, "key", null));
        producerClient.send(new ProducerRecord<>(TEST_TOPIC, null, "value"));
        producerClient.send(new ProducerRecord<>(TEST_TOPIC, "key", "value"));

        consumerClient.start();
        Thread.sleep(500);
        consumerClient.stop();

        assertEquals(5, counter.get());
    }

    @Test
    public void should_produce_record_sync() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(TEST_TOPIC, (record) -> {
                    counter.incrementAndGet();
                    return ConsumeStatus.OK;
                })
        ));

        consumerClient.start();

        producerClient.sendSync(new ProducerRecord<>(TEST_TOPIC, "key", "value"));

        Thread.sleep(500);

        consumerClient.stop();

        assertEquals(1, counter.get());
    }

    @Test
    public void should_throw_exception_when_fail_to_send_sync() {
        kafka.stop();

        assertThrows(ExecutionException.class, () -> {
            producerClient.sendSync(new ProducerRecord<>(TEST_TOPIC, "key", "value"));
        });
    }

    @Test
    public void should_execute_callback_with_exception_when_fail_to_send() throws InterruptedException {
        kafka.stop();

        AtomicReference<Exception> exceptionRef = new AtomicReference<>();

        producerClient.send(new ProducerRecord<>(TEST_TOPIC, "key", "value"), (metadata, exception) -> {
            exceptionRef.set(exception);
        });

        Thread.sleep(2500);

        assertEquals(TimeoutException.class, exceptionRef.get().getClass());
    }

    @Test
    public void should_execute_callback_with_exception_when_sending_after_closed() {
        AtomicInteger counter = new AtomicInteger();

        Callback callback = (metadata, exception) -> {
            if (exception != null) {
                counter.incrementAndGet();
            }
        };

        producerClient.close();

        producerClient.send(new ProducerRecord<>(TEST_TOPIC, "key", "value"), callback);
        producerClient.send(new ProducerRecord<>(TEST_TOPIC, "key", "value"), callback);

        assertEquals(2, counter.get());
    }

    @Test
    public void should_throw_exception_when_sending_after_closed() {
        producerClient.close();

        assertThrows(ExecutionException.class, () -> producerClient.sendSync(new ProducerRecord<>(TEST_TOPIC, "key", "value")));
    }

}
