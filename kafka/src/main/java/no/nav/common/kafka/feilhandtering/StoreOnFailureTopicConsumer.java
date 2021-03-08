package no.nav.common.kafka.feilhandtering;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.feilhandtering.db.KafkaRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class StoreOnFailureTopicConsumer implements TopicConsumer<String, String> {

    private final KafkaRepository kafkaRepository;

    private final TopicConsumer<String, String> mainConsumer;

    public StoreOnFailureTopicConsumer(KafkaRepository kafkaRepository, TopicConsumer<String, String> mainConsumer) {
        this.kafkaRepository = kafkaRepository;
        this.mainConsumer = mainConsumer;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, String> record) {
        ConsumeStatus status;

        try {
            status = mainConsumer.consume(record);
        } catch (Exception e) {
            // TODO: log warn
            status = ConsumeStatus.FAILED;
        }

        if (status == ConsumeStatus.OK) {
            return ConsumeStatus.OK;
        }

        try {
            // TODO: Should check that the record is not already stored before saving
            //  Check on topic + partition + offset

            // TODO: Store in database
            return ConsumeStatus.OK;
        } catch (Exception e) {
            // TODO: log error
            return ConsumeStatus.FAILED;
        }
    }
}
