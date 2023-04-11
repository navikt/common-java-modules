package no.nav.common.kafka.consumer.util;

import no.nav.common.kafka.consumer.KafkaConsumerClient;
import org.junit.Test;

import java.time.Duration;
import java.util.function.Supplier;

import static org.awaitility.Awaitility.*;
import static org.mockito.Mockito.*;

public class FeatureToggledKafkaConsumerClientTest {

    @Test
    public void should_start_consumer_when_toggle_is_off_and_consumer_is_not_running() {
        KafkaConsumerClient consumerClient = mock(KafkaConsumerClient.class);
        Supplier<Boolean> toggleForStoppingConsumersSupplier = mock(Supplier.class);

        when(consumerClient.isRunning()).thenReturn(false);
        when(toggleForStoppingConsumersSupplier.get()).thenReturn(false);

        FeatureToggledKafkaConsumerClient toggledClient = new FeatureToggledKafkaConsumerClient(consumerClient, toggleForStoppingConsumersSupplier, Duration.ofMillis(10));


        toggledClient.start();

        when(consumerClient.isRunning()).thenReturn(true);


        toggledClient.stop();

        when(consumerClient.isRunning()).thenReturn(false);


        await().ignoreExceptions().atMost(Duration.ofMillis(1000)).until( () -> {
            verify(consumerClient, atLeastOnce()).start();
            return true;
        });
    }

    @Test
    public void should_stop_consumer_when_toggle_is_on_and_consumer_is_running() {
        KafkaConsumerClient consumerClient = mock(KafkaConsumerClient.class);
        Supplier<Boolean> toggleForStoppingConsumersSupplier = mock(Supplier.class);
        when(toggleForStoppingConsumersSupplier.get()).thenReturn(false);

        when(consumerClient.isRunning()).thenReturn(false);

        FeatureToggledKafkaConsumerClient toggledClient = new FeatureToggledKafkaConsumerClient(consumerClient, toggleForStoppingConsumersSupplier, Duration.ofMillis(10));

        toggledClient.start();

        when(consumerClient.isRunning()).thenReturn(true);

        when(toggleForStoppingConsumersSupplier.get()).thenReturn(true);

        await().ignoreExceptions().atMost(Duration.ofMillis(1000)).until( () -> {
            verify(consumerClient, atLeastOnce()).stop();
            return true;
        });
        toggledClient.stop();

    }

    @Test
    public void should_not_stop_consumer_when_toggle_is_off_and_consumer_is_running() {
        KafkaConsumerClient consumerClient = mock(KafkaConsumerClient.class);
        Supplier<Boolean> toggleForStoppingConsumersSupplier = mock(Supplier.class);
        when(toggleForStoppingConsumersSupplier.get()).thenReturn(false);

        when(consumerClient.isRunning()).thenReturn(false);

        FeatureToggledKafkaConsumerClient toggledClient = new FeatureToggledKafkaConsumerClient(consumerClient, toggleForStoppingConsumersSupplier, Duration.ofMillis(10));

        toggledClient.start();

        when(consumerClient.isRunning()).thenReturn(true);

        await().ignoreExceptions().atMost(Duration.ofMillis(1000)).until( () -> {
            verify(consumerClient, never()).stop();
            return true;
        });
        toggledClient.stop();
    }

}
