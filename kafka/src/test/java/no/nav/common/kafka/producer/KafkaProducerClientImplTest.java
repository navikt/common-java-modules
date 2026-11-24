package no.nav.common.kafka.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.kafka.producer.util.KafkaProducerClientWithMetrics;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

public class KafkaProducerClientImplTest {

    @Test
    public void skal_bevare_getProducer_og_tillate_subklasser() {
        var producer = new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
        KafkaProducerClient<String, String> client = new KafkaProducerClientImpl<>(producer) {};
        var registry = new SimpleMeterRegistry();
        var metricsClient = new KafkaProducerClientWithMetrics<>(client, registry);
        try {
            assertSame(producer, client.getProducer());
            assertSame(producer, metricsClient.getProducer());
        } finally {
            try {
                metricsClient.close();
            } finally {
                registry.close();
            }
        }
    }

    @Test
    public void skal_bevare_standardhook_og_kunne_velge_hook_i_konstruktorene() throws Exception {
        var registry = new SimpleMeterRegistry();
        try {
            try (var producer = new GracefulKafkaProducer<String, String>(producerProperties())) {
                bekreftShutdownHook(producer, true);
            }
            bekreftKlientHook(new KafkaProducerClientImpl<String, String>(producerProperties()), true);
            bekreftKlientHook(new KafkaProducerClientWithMetrics<String, String>(producerProperties(), registry), true);
            for (boolean enabled : new boolean[] {false, true}) {
                try (var producer = new GracefulKafkaProducer<String, String>(producerProperties(), enabled)) {
                    bekreftShutdownHook(producer, enabled);
                }
                bekreftKlientHook(new KafkaProducerClientImpl<String, String>(producerProperties(), enabled), enabled);
                bekreftKlientHook(
                        new KafkaProducerClientWithMetrics<String, String>(producerProperties(), registry, enabled),
                        enabled);
            }
        } finally {
            registry.close();
        }
    }

    @Test
    public void skal_velge_shutdown_hook_gjennom_builder_ogsa_med_metrikker() throws Exception {
        for (boolean metrics : new boolean[] {false, true}) {
            var registry = new SimpleMeterRegistry();
            try {
                for (int mode = 0; mode < 3; mode++) {
                    var builder =
                            KafkaProducerClientBuilder.<String, String>builder().withProperties(producerProperties());
                    if (mode == 1) {
                        builder.withShutdownHookEnabled(false).withShutdownHookEnabled(true);
                    } else if (mode == 2) {
                        builder.withShutdownHookEnabled(false);
                    }
                    if (metrics) {
                        builder.withMetrics(registry);
                    }
                    bekreftKlientHook(builder.build(), mode != 2);
                }
            } finally {
                registry.close();
            }
        }
    }

    @Test
    public void should_send_message_with_producer_async() {
        MockProducer<String, String> mockProducer =
                new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
        KafkaProducerClientImpl<String, String> client = new KafkaProducerClientImpl<>(mockProducer);

        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key", "value");

        client.send(record);

        assertEquals(record, mockProducer.history().getFirst());
    }

    @Test
    public void should_send_message_with_producer_sync() {
        MockProducer<String, String> mockProducer =
                new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
        KafkaProducerClientImpl<String, String> client = new KafkaProducerClientImpl<>(mockProducer);

        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key", "value");

        client.sendSync(record);

        assertEquals(record, mockProducer.history().getFirst());
    }

    @Test
    public void should_trigger_callback_with_exception_if_producer_is_closed() {
        MockProducer<String, String> mockProducer =
                new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
        KafkaProducerClientImpl<String, String> client = new KafkaProducerClientImpl<>(mockProducer);

        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key", "value");

        client.close();

        AtomicReference<Exception> exceptionRef = new AtomicReference<>();

        client.send(record, (metadata, exception) -> exceptionRef.set(exception));

        assertEquals(IllegalStateException.class, exceptionRef.get().getClass());
    }

    private static Properties producerProperties() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return properties;
    }

    private static void bekreftShutdownHook(Producer<?, ?> producer, boolean expected) throws Exception {
        Field field = GracefulKafkaProducer.class.getDeclaredField("shutdownHook");
        field.setAccessible(true);
        Thread hook = (Thread) field.get(producer);
        if (hook != null) {
            assertTrue(Runtime.getRuntime().removeShutdownHook(hook));
        }
        assertEquals(expected, hook != null);
    }

    private static void bekreftKlientHook(KafkaProducerClient<?, ?> client, boolean expected) throws Exception {
        try {
            bekreftShutdownHook(client.getProducer(), expected);
        } finally {
            client.close();
        }
    }
}
