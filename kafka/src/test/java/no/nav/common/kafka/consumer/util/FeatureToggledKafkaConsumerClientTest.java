package no.nav.common.kafka.consumer.util;

import no.nav.common.kafka.consumer.KafkaConsumerClient;
import org.junit.Test;

import java.time.Duration;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.awaitility.Awaitility.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FeatureToggledKafkaConsumerClientTest {

    @Test
    public void disabled_start_is_idempotent_but_keeps_scheduler_alive_and_can_restart() throws Exception {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        Supplier<Boolean> toggle = mock(Supplier.class);
        when(toggle.get()).thenReturn(true);
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, toggle, Duration.ofDays(1));
        client.start();
        ScheduledExecutorService first = scheduler(client);
        try {
            client.start();
            assertSame(first, scheduler(client));
            assertFalse(first.isShutdown());
            verify(toggle).get();
            verify(underlying, never()).start();
        } finally {
            client.stop();
        }
        client.stop();
        assertTrue(first.isTerminated());
        verify(underlying).stop();
        client.start();
        assertNotSame(first, scheduler(client));
        client.stop();
    }

    @Test
    public void initially_disabled_client_resumes_and_pauses_when_toggle_changes() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        AtomicBoolean disabled = new AtomicBoolean(true);
        AtomicBoolean running = new AtomicBoolean();
        when(underlying.isRunning()).thenAnswer(invocation -> running.get());
        doAnswer(invocation -> { running.set(true); return null; }).when(underlying).start();
        doAnswer(invocation -> { running.set(false); return null; }).when(underlying).stop();
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, disabled::get, Duration.ofMillis(1));
        client.start();
        try {
            disabled.set(false);
            await().atMost(Duration.ofSeconds(2)).until(running::get);
            disabled.set(true);
            await().atMost(Duration.ofSeconds(2)).until(() -> !running.get());
        } finally {
            client.stop();
        }
    }

    @Test
    public void supplier_failure_is_retried_instead_of_silently_disabling_scheduler() throws Exception {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        doAnswer(invocation -> { started.countDown(); return null; }).when(underlying).start();
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> {
            int call = calls.incrementAndGet();
            if (call == 2) {
                throw new IllegalStateException("toggle unavailable");
            }
            return call == 1;
        }, Duration.ofMillis(1));
        client.start();
        try {
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertTrue(calls.get() >= 3);
        } finally {
            client.stop();
        }
    }

    @Test
    public void scheduled_start_failure_cleans_underlying_before_retrying() throws Exception {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        AtomicInteger toggleCalls = new AtomicInteger();
        CountDownLatch restarted = new CountDownLatch(1);
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger stops = new AtomicInteger();
        doAnswer(invocation -> {
            if (starts.incrementAndGet() == 1) {
                throw new IllegalStateException("partial startup");
            }
            assertEquals(1, stops.get());
            restarted.countDown();
            return null;
        }).when(underlying).start();
        doAnswer(invocation -> { stops.incrementAndGet(); return null; }).when(underlying).stop();
        when(underlying.isRunning()).thenAnswer(invocation -> starts.get() > 1);
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(
                underlying, () -> toggleCalls.incrementAndGet() == 1, Duration.ofMillis(1));
        client.start();
        try {
            assertTrue(restarted.await(2, TimeUnit.SECONDS));
            verify(underlying, times(2)).start();
        } finally {
            client.stop();
        }
    }

    @Test
    public void failed_start_always_stops_underlying_even_when_not_running_and_preserves_suppressed_failure() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        IllegalArgumentException failure = new IllegalArgumentException("start");
        IllegalStateException cleanupFailure = new IllegalStateException("stop");
        doThrow(failure).when(underlying).start();
        doThrow(cleanupFailure).doNothing().when(underlying).stop();
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> false);
        assertSame(failure, assertThrows(IllegalArgumentException.class, client::start));
        assertArrayEquals(new Throwable[]{cleanupFailure}, failure.getSuppressed());
        verify(underlying).stop();
        assertThrows(IllegalStateException.class, client::start);
        client.stop();
        verify(underlying, times(2)).stop();
    }

    @Test
    public void failed_scheduler_start_cleans_scheduler_and_underlying() throws Exception {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(
                underlying, () -> false, Duration.ZERO);
        assertThrows(IllegalArgumentException.class, client::start);
        assertTrue(scheduler(client).isTerminated());
        verify(underlying).start();
        verify(underlying).stop();
        client.stop();
        verify(underlying).stop();
    }

    @Test
    public void same_start_and_cleanup_failure_is_not_added_as_its_own_suppressed_exception() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        RuntimeException failure = new IllegalArgumentException("start and stop");
        doThrow(failure).when(underlying).start();
        doThrow(failure).doNothing().when(underlying).stop();
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> false);
        assertSame(failure, assertThrows(IllegalArgumentException.class, client::start));
        assertEquals(0, failure.getSuppressed().length);
        client.stop();
    }

    @Test
    public void failed_initial_toggle_read_still_cleans_underlying() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> {
            throw new IllegalStateException("toggle unavailable");
        });
        assertThrows(IllegalStateException.class, client::start);
        verify(underlying).stop();
        client.stop();
        verify(underlying).stop();
    }

    @Test
    public void scheduler_timeout_prevents_late_start_and_allows_stop_retry() throws Exception {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        FeatureToggledKafkaConsumerClient client = blockedScheduler(underlying, entered, release, 20);
        client.start();
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertThrows(IllegalStateException.class, client::stop);
            assertThrows(IllegalStateException.class, client::stop);
            assertThrows(IllegalStateException.class, client::start);
            verify(underlying, never()).stop();
        } finally {
            release.countDown();
            await().atMost(Duration.ofSeconds(2)).until(() -> scheduler(client).isTerminated());
            client.stop();
        }
        verify(underlying, never()).start();
        verify(underlying).stop();
    }

    @Test
    public void interrupted_scheduler_stop_restores_interrupt_and_can_be_retried() throws Exception {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        FeatureToggledKafkaConsumerClient client = blockedScheduler(underlying, entered, release, 1000);
        client.start();
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            Thread.currentThread().interrupt();
            IllegalStateException failure = assertThrows(IllegalStateException.class, client::stop);
            assertTrue(failure.getCause() instanceof InterruptedException);
            assertTrue(Thread.currentThread().isInterrupted());
            verify(underlying, never()).stop();
        } finally {
            Thread.interrupted();
            release.countDown();
            client.stop();
        }
        verify(underlying).stop();
    }

    @Test
    public void underlying_stop_failure_is_retried_even_if_underlying_reports_not_running() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        doThrow(new IllegalStateException("still stopping")).doNothing().when(underlying).stop();
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> true);
        client.start();
        assertThrows(IllegalStateException.class, client::stop);
        assertThrows(IllegalStateException.class, client::start);
        client.stop();
        verify(underlying, times(2)).stop();
    }

    @Test
    public void concurrent_start_waits_for_underlying_stop_to_complete() throws Exception {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        CountDownLatch stopping = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            stopping.countDown();
            assertTrue(release.await(3, TimeUnit.SECONDS));
            return null;
        }).when(underlying).stop();
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> false, Duration.ofDays(1));
        ExecutorService callers = Executors.newFixedThreadPool(2);
        client.start();
        try {
            Future<?> stop = callers.submit(client::stop);
            assertTrue(stopping.await(2, TimeUnit.SECONDS));
            CountDownLatch startEntered = new CountDownLatch(1);
            Future<?> start = callers.submit(() -> {
                startEntered.countDown();
                client.start();
            });
            assertTrue(startEntered.await(2, TimeUnit.SECONDS));
            assertFalse(stop.isDone());
            assertFalse(start.isDone());
            release.countDown();
            stop.get(2, TimeUnit.SECONDS);
            start.get(2, TimeUnit.SECONDS);
            verify(underlying, times(2)).start();
        } finally {
            release.countDown();
            callers.shutdown();
            assertTrue(callers.awaitTermination(2, TimeUnit.SECONDS));
            client.stop();
        }
    }

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

    private static FeatureToggledKafkaConsumerClient blockedScheduler(KafkaConsumerClient underlying,
            CountDownLatch entered, CountDownLatch release, long timeout) {
        AtomicInteger calls = new AtomicInteger();
        return new FeatureToggledKafkaConsumerClient(underlying, () -> {
            if (calls.incrementAndGet() == 1) {
                return true;
            }
            entered.countDown();
            try {
                assertTrue(release.await(3, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Toggle task interrupted", e);
            }
            return false;
        }, Duration.ofMillis(1), timeout);
    }

    private static ScheduledExecutorService scheduler(FeatureToggledKafkaConsumerClient client) throws Exception {
        Field field = FeatureToggledKafkaConsumerClient.class.getDeclaredField("executorService");
        field.setAccessible(true);
        return (ScheduledExecutorService) field.get(client);
    }

}
