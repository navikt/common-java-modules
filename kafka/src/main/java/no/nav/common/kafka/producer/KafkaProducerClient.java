package no.nav.common.kafka.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// TODO: Should implement interface for easier mocking
public class KafkaProducerClient {

    // TODO: Mulig at vi må ha støtte for at man skal kunne spesifisere annet enn <String, String>
    private final Producer<String, String> producer;

    // TODO: Kunne vurdert å ha en liste med listeners slik at man kan gjøre flere ting, som f.eks metrikker + feilhandtering
    private final Map<String, KafkaProducerListener> topicListeners;

    public KafkaProducerClient(KafkaProducerClientConfig config) {
        producer = new KafkaProducer<>(config.properties);
        topicListeners = config.topicListeners;
        // TODO: Create executor and run consume()
    }

    public void send(ProducerRecord<String, String> record) throws ExecutionException, InterruptedException, TimeoutException {
        // ProducerRecord<String, String> record = new ProducerRecord<>("java_topic", null, recordValue);

        try {
            producer.send(record).get(1000L, TimeUnit.MILLISECONDS); // Blocks for max 1 second or fails
            // TODO: Invoke listener onSuccess
        } catch (Exception e) {
            // TODO: Invoke listener onError
        }

    }

    public void close() {
        producer.close();
    }

}
