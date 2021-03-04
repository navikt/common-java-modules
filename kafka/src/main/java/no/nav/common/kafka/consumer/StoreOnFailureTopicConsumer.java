package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public class StoreOnFailureTopicConsumer implements TopicConsumer<String, String> {

    private final TopicConsumer<String, String> mainConsumer;

    public StoreOnFailureTopicConsumer(TopicConsumer<String, String> mainConsumer) {
        this.mainConsumer = mainConsumer;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, String> record) {
        try {
            ConsumeStatus status = mainConsumer.consume(record);

            if (status == ConsumeStatus.FAILED) {
                try {
                    // TODO: Store in database
                    return ConsumeStatus.OK;
                } catch (Exception e) {
                    return ConsumeStatus.FAILED;
                }
            }

            return status;
        } catch (Exception e) {
            return ConsumeStatus.FAILED;
        }

    }
}
