package no.nav.common.kafka.domain;

public class KafkaMessage {
    public final long id;
    public final String topic;
    public final String key;
    public final String value;

    public KafkaMessage(long id, String topic, String key, String value) {
        this.id = id;
        this.topic = topic;
        this.key = key;
        this.value = value;
    }
}
