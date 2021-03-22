package no.nav.common.kafka.util;

import no.nav.common.utils.Credentials;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class KafkaProperties {

    public static Properties onPremDefaultConsumerProperties(String consumerGroupId, String kafkaBrokersUrl, Credentials credentials) {
        return KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId(consumerGroupId)
                .withBrokerUrl(kafkaBrokersUrl)
                .withOnPremAuth(credentials)
                .withDeserializers(StringDeserializer.class, StringDeserializer.class)
                .build();
    }

    public static Properties onPremDefaultProducerProperties(String producerId, String kafkaBrokersUrl, Credentials credentials) {
        return KafkaPropertiesBuilder.producerBuilder()
                .withBaseProperties()
                .withProducerId(producerId)
                .withBrokerUrl(kafkaBrokersUrl)
                .withOnPremAuth(credentials)
                .withSerializers(StringSerializer.class, StringSerializer.class)
                .build();
    }

    public static Properties onPremByteProducerProperties(String producerId, String kafkaBrokersUrl, Credentials credentials) {
        return KafkaPropertiesBuilder.producerBuilder()
                .withBaseProperties()
                .withProducerId(producerId)
                .withBrokerUrl(kafkaBrokersUrl)
                .withOnPremAuth(credentials)
                .withSerializers(ByteArraySerializer.class, ByteArraySerializer.class)
                .build();
    }

}
