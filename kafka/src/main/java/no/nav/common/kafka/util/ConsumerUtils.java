package no.nav.common.kafka.util;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;

import java.util.List;

public class ConsumerUtils {

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

}
