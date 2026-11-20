package no.nav.common.kafka.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Properties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.Test;

public class KafkaPropertiesBuilderTest {

    @Test
    public void skal_oke_idle_timeout_nar_poll_interval_overskrider_kafka_standardverdien() {
        Properties properties = KafkaPropertiesBuilder.consumerBuilder()
                .withBrokerUrl("localhost:9092")
                .withConsumerGroupId("test-consumer")
                .withPollProperties(500, 750_000)
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .build();

        assertEquals(750_000, properties.get(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG));
        assertEquals(750_000, properties.get(CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG));
    }

    @Test
    public void skal_fjerne_automatisk_idle_timeout_nar_poll_interval_forkortes() {
        Properties properties = KafkaPropertiesBuilder.consumerBuilder()
                .withBrokerUrl("localhost:9092")
                .withConsumerGroupId("test-consumer")
                .withBaseProperties()
                .withPollProperties(500, 300_000)
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .build();

        assertFalse(properties.containsKey(CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG));
    }

    @Test
    public void skal_oppdatere_idle_timeout_nar_poll_interval_overstyres_med_withProp() {
        Properties properties = KafkaPropertiesBuilder.consumerBuilder()
                .withBrokerUrl("localhost:9092")
                .withConsumerGroupId("test-consumer")
                .withBaseProperties()
                .withProp(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 900_000)
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .build();

        assertEquals(900_000, properties.get(CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG));
    }

    @Test
    public void skal_oppdatere_idle_timeout_nar_poll_interval_overstyres_med_withProps() {
        Properties pollProperties = new Properties();
        pollProperties.setProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "900000");

        Properties properties = KafkaPropertiesBuilder.consumerBuilder()
                .withBrokerUrl("localhost:9092")
                .withConsumerGroupId("test-consumer")
                .withBaseProperties()
                .withProps(pollProperties)
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .build();

        assertEquals(900_000, properties.get(CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG));
    }

    @Test
    public void skal_beholde_eksplisitt_idle_timeout_satt_med_withProp() {
        Properties properties = KafkaPropertiesBuilder.consumerBuilder()
                .withBrokerUrl("localhost:9092")
                .withConsumerGroupId("test-consumer")
                .withBaseProperties()
                .withProp(CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG, 900_000)
                .withPollProperties(500, 300_000)
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .build();

        assertEquals(900_000, properties.get(CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG));
    }

    @Test
    public void skal_beholde_eksplisitt_idle_timeout_satt_med_withProps() {
        Properties explicitProperties = new Properties();
        explicitProperties.put(CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG, 900_000);

        Properties properties = KafkaPropertiesBuilder.consumerBuilder()
                .withBrokerUrl("localhost:9092")
                .withConsumerGroupId("test-consumer")
                .withProps(explicitProperties)
                .withBaseProperties()
                .withPollProperties(500, 300_000)
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .build();

        assertEquals(900_000, properties.get(CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG));
    }
}
