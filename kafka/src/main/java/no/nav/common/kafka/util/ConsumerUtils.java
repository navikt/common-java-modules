package no.nav.common.kafka.util;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.domain.KafkaConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.lang.String.format;

public class ConsumerUtils {

    private final static Logger log = LoggerFactory.getLogger(ConsumerUtils.class);

    public static <K, V> TopicConsumer<K, V> aggregateConsumer(final List<TopicConsumer<K, V>> consumers) {
        return record -> {
            ConsumeStatus aggregatedStatus = ConsumeStatus.OK;

            for (TopicConsumer<K, V> consumer : consumers) {
                ConsumeStatus status = consumer.consume(record);

                if (status == ConsumeStatus.FAILED) {
                    aggregatedStatus = ConsumeStatus.FAILED;
                }
            }

            return aggregatedStatus;
        };
    }

    public static <K, V> ConsumeStatus safeConsume(TopicConsumer<K, V> topicConsumer, ConsumerRecord<K, V> consumerRecord) {
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

    public static <K, V> ConsumerRecord<K, V> mapRecord(KafkaConsumerRecord<K, V> record) {
        return new ConsumerRecord<>(record.topic, record.partition, record.offset, record.key, record.value);
    }

}
