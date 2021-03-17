package no.nav.common.kafka.consumer.feilhandtering;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class StoreRecordTopicConsumer implements TopicConsumer<byte[], byte[]> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final KafkaConsumerRepository consumerRepository;

    public StoreRecordTopicConsumer(KafkaConsumerRepository consumerRepository) {
        this.consumerRepository = consumerRepository;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<byte[], byte[]> record) {
        try {
            consumerRepository.storeRecord(ConsumerUtils.mapRecord(record));
            log.info("Stored consumer record topic={} partition={} offset={}", record.topic(), record.partition(), record.offset());
            return ConsumeStatus.OK;
        } catch (Exception e) {
            String msg = format(
                    "Failed to store consumer record topic=%s, partition=%d offset=%d",
                    record.topic(), record.partition(), record.offset()
            );

            log.error(msg, e);
            return ConsumeStatus.FAILED;
        }
    }
}
