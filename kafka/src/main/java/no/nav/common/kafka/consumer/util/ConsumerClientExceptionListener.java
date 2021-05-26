package no.nav.common.kafka.consumer.util;

public interface ConsumerClientExceptionListener {

    void onExceptionCaught(Exception e);

}
