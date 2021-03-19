package no.nav.common.kafka.util;

import no.nav.common.utils.Credentials;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class KafkaProperties {

    public static Properties defaultOnPremConsumerProperties(String consumerGroupId, String kafkaBrokersUrl, Credentials credentials) {
        return KafkaPropertiesBuilder.builder()
                .withBaseConsumerProperties()
                .withConsumerGroupId(consumerGroupId)
                .withBrokerUrl(kafkaBrokersUrl)
                .withOnPremAuth(credentials)
                .withConsumerDeserializers(StringDeserializer.class, StringDeserializer.class)
                .build();
    }

    public static Properties defaultOnPremProducerProperties(String producerId, String kafkaBrokersUrl, Credentials credentials) {
        return KafkaPropertiesBuilder.builder()
                .withBaseProducerProperties()
                .withProducerId(producerId)
                .withBrokerUrl(kafkaBrokersUrl)
                .withOnPremAuth(credentials)
                .withProducerSerializers(StringSerializer.class, StringSerializer.class)
                .build();
    }

    public static Properties byteOnPremProducerProperties(String producerId, String kafkaBrokersUrl, Credentials credentials) {
        return KafkaPropertiesBuilder.builder()
                .withBaseProducerProperties()
                .withProducerId(producerId)
                .withBrokerUrl(kafkaBrokersUrl)
                .withOnPremAuth(credentials)
                .withProducerSerializers(ByteArraySerializer.class, ByteArraySerializer.class)
                .build();
    }

}
