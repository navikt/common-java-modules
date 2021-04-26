package no.nav.common.kafka.util;

import no.nav.common.utils.Credentials;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class KafkaPropertiesPreset {

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


    public static Properties aivenDefaultConsumerProperties(String consumerGroupId) {
        return KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId(consumerGroupId)
                .withAivenBrokerUrl()
                .withAivenAuth()
                .withDeserializers(StringDeserializer.class, StringDeserializer.class)
                .build();
    }

    public static Properties aivenDefaultProducerProperties(String producerId) {
        return KafkaPropertiesBuilder.producerBuilder()
                .withBaseProperties()
                .withProducerId(producerId)
                .withAivenBrokerUrl()
                .withAivenAuth()
                .withSerializers(StringSerializer.class, StringSerializer.class)
                .build();
    }

    public static Properties aivenByteProducerProperties(String producerId) {
        return KafkaPropertiesBuilder.producerBuilder()
                .withBaseProperties()
                .withProducerId(producerId)
                .withAivenBrokerUrl()
                .withAivenAuth()
                .withSerializers(ByteArraySerializer.class, ByteArraySerializer.class)
                .build();
    }

}
