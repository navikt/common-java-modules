package no.nav.common.kafka.consumer.util;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import org.junit.Test;

public class FeatureToggledKafkaConsumerClientTest {

    @Test
    public void should_start_consumer_when_toggle_is_off_and_consumer_is_not_running() {
        KafkaConsumerClient consumerClient = mock(KafkaConsumerClient.class);
        AtomicBoolean toggleForStoppingConsumers = new AtomicBoolean(true);

        when(consumerClient.isRunning()).thenReturn(false);

        FeatureToggledKafkaConsumerClient toggledClient = new FeatureToggledKafkaConsumerClient(
                consumerClient, toggleForStoppingConsumers::get, Duration.ofMillis(10));

        try {
            toggledClient.start();
            toggleForStoppingConsumers.set(false);
            await().atMost(Duration.ofSeconds(1))
                    .untilAsserted(() -> verify(consumerClient, atLeastOnce()).start());
        } finally {
            toggledClient.stop();
        }
    }

    @Test
    public void should_stop_consumer_when_toggle_is_on_and_consumer_is_running() {
        KafkaConsumerClient consumerClient = mock(KafkaConsumerClient.class);
        AtomicBoolean toggleForStoppingConsumers = new AtomicBoolean(false);

        when(consumerClient.isRunning()).thenReturn(false);

        FeatureToggledKafkaConsumerClient toggledClient = new FeatureToggledKafkaConsumerClient(
                consumerClient, toggleForStoppingConsumers::get, Duration.ofMillis(10));

        try {
            toggledClient.start();
            when(consumerClient.isRunning()).thenReturn(true);
            toggleForStoppingConsumers.set(true);
            await().atMost(Duration.ofSeconds(1))
                    .untilAsserted(() -> verify(consumerClient, atLeastOnce()).stop());
        } finally {
            toggledClient.stop();
        }
    }

    @Test
    public void should_not_stop_consumer_when_toggle_is_off_and_consumer_is_running() {
        KafkaConsumerClient consumerClient = mock(KafkaConsumerClient.class);
        AtomicBoolean toggleForStoppingConsumers = new AtomicBoolean(true);
        AtomicInteger pollsWithToggleOff = new AtomicInteger();

        when(consumerClient.isRunning()).thenReturn(false);

        FeatureToggledKafkaConsumerClient toggledClient = new FeatureToggledKafkaConsumerClient(
                consumerClient,
                () -> {
                    boolean toggledOff = toggleForStoppingConsumers.get();
                    if (!toggledOff) {
                        pollsWithToggleOff.incrementAndGet();
                    }
                    return toggledOff;
                },
                Duration.ofMillis(10));

        try {
            toggledClient.start();
            when(consumerClient.isRunning()).thenReturn(true);
            toggleForStoppingConsumers.set(false);
            await().atMost(Duration.ofSeconds(1)).until(() -> pollsWithToggleOff.get() > 0);
            await().during(Duration.ofMillis(50))
                    .atMost(Duration.ofSeconds(1))
                    .untilAsserted(() -> verify(consumerClient, never()).stop());
        } finally {
            toggledClient.stop();
        }
    }
}
