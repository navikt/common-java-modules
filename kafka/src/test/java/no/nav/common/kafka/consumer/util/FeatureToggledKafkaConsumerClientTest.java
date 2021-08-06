package no.nav.common.kafka.consumer.util;

import no.nav.common.kafka.consumer.KafkaConsumerClient;
import org.junit.Test;

import java.time.Duration;
import java.util.function.Supplier;

import static org.mockito.Mockito.*;

public class FeatureToggledKafkaConsumerClientTest {

    @Test
    public void should_start_consumer_when_toggle_is_off_and_consumer_is_not_running() throws InterruptedException {
        KafkaConsumerClient consumerClient = mock(KafkaConsumerClient.class);
        Supplier<Boolean> toggleForStoppingConsumersSupplier = mock(Supplier.class);

        when(consumerClient.isRunning()).thenReturn(false);
        when(toggleForStoppingConsumersSupplier.get()).thenReturn(false);

        FeatureToggledKafkaConsumerClient toggledClient = new FeatureToggledKafkaConsumerClient(consumerClient, toggleForStoppingConsumersSupplier, Duration.ofMillis(10));


        toggledClient.start();

        when(consumerClient.isRunning()).thenReturn(true);


        toggledClient.stop();

        when(consumerClient.isRunning()).thenReturn(false);


        Thread.sleep(20);

        verify(consumerClient, atLeastOnce()).start();
    }

    @Test
    public void should_stop_consumer_when_toggle_is_on_and_consumer_is_running() throws InterruptedException {
        KafkaConsumerClient consumerClient = mock(KafkaConsumerClient.class);
        Supplier<Boolean> toggleForStoppingConsumersSupplier = mock(Supplier.class);

        when(consumerClient.isRunning()).thenReturn(false);

        FeatureToggledKafkaConsumerClient toggledClient = new FeatureToggledKafkaConsumerClient(consumerClient, toggleForStoppingConsumersSupplier, Duration.ofMillis(10));

        toggledClient.start();

        when(consumerClient.isRunning()).thenReturn(true);

        when(toggleForStoppingConsumersSupplier.get()).thenReturn(true);

        Thread.sleep(20);

        verify(consumerClient, atLeastOnce()).stop();
    }

    @Test
    public void should_not_stop_consumer_when_toggle_is_off_and_consumer_is_running() throws InterruptedException {
        KafkaConsumerClient consumerClient = mock(KafkaConsumerClient.class);
        Supplier<Boolean> toggleForStoppingConsumersSupplier = mock(Supplier.class);

        when(consumerClient.isRunning()).thenReturn(false);

        FeatureToggledKafkaConsumerClient toggledClient = new FeatureToggledKafkaConsumerClient(consumerClient, toggleForStoppingConsumersSupplier, Duration.ofMillis(10));

        toggledClient.start();

        when(consumerClient.isRunning()).thenReturn(true);

        when(toggleForStoppingConsumersSupplier.get()).thenReturn(false);

        Thread.sleep(20);

        verify(consumerClient, never()).stop();
    }

}
