package no.nav.common.kafka.util;

import no.nav.common.utils.Credentials;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class KafkaProperties {

    public static Properties defaultConsumerProperties(String consumerGroupId, String kafkaBrokersUrl, Credentials credentials) {
        Properties props = new Properties();

        props.putAll(brokersUrlProperty(kafkaBrokersUrl));
        props.putAll(onPremKafkaAuthProperties(credentials));

        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

        /*
            Where to start when reading from a new topic that has not been consumed by this consumer.
            Earliest = start from the first message in the topic
         */
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        /*
            If a batch takes longer than the time specified then the consumer will be assumed dead and another consumer will be assigned.
            30 minutes
         */
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 30 * 60 * 1000);

        /*
            KafkaConsumerClient commits manually, so this must be turned off.
         */
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        /*
            How many records can max be returned for each poll().
            KafkaConsumerClient processes records asynchronously and loads all records into memory,
            so it is important that the application can handle the maximum specified.
            If faster processing is needed, the poll timeout can also be adjusted.
         */
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 5000);

        /*
            Deserialize the key and value from each message as a string.
         */
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return props;
    }

    public static Properties defaultProducerProperties(String producerGroupId, String kafkaBrokersUrl, Credentials credentials) {
        Properties props = new Properties();

        props.putAll(brokersUrlProperty(kafkaBrokersUrl));
        props.putAll(onPremKafkaAuthProperties(credentials));

        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, producerGroupId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return props;
    }

    public static Properties brokersUrlProperty(String kafkaBrokersUrl) {
        Properties props = new Properties();

        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokersUrl);

        return props;
    }

    public static Properties onPremKafkaAuthProperties(Credentials credentials) {
        Properties props = new Properties();

        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");

        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + credentials.username + "\" password=\"" + credentials.password + "\";");

        return props;
    }

}
