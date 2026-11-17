package no.nav.common.kafka.spring;

import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.FeatureToggledKafkaConsumerClient;
import org.junit.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KafkaConsumerClientLifecycleTest {

    @Test
    public void construction_and_unmanaged_context_do_not_start_consumers() {
        KafkaConsumerClient client = mock(KafkaConsumerClient.class);
        KafkaConsumerClientLifecycle lifecycle = new KafkaConsumerClientLifecycle(List.of(client));
        assertFalse(lifecycle.isRunning());
        assertTrue(lifecycle.isAutoStartup());
        assertEquals(Integer.MAX_VALUE, lifecycle.getPhase());
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean("client", KafkaConsumerClient.class, () -> client,
                    definition -> definition.setDestroyMethodName(""));
            context.refresh();
        }
        verifyNoInteractions(client);
    }

    @Test
    public void refresh_starts_after_dependencies_and_close_stops_before_their_destruction() {
        assertContextOrdering(Integer.MAX_VALUE - 1);
    }

    @Test
    public void explicit_dependency_orders_infrastructure_in_the_same_phase() {
        assertContextOrdering(Integer.MAX_VALUE);
    }

    private void assertContextOrdering(int infrastructurePhase) {
        List<String> events = new ArrayList<>();
        KafkaConsumerClient client = mock(KafkaConsumerClient.class);
        Infrastructure infrastructure = new Infrastructure(events, infrastructurePhase);
        doAnswer(invocation -> {
            assertTrue(infrastructure.initialized);
            assertTrue(infrastructure.isRunning());
            events.add("consumer.start");
            return null;
        }).when(client).start();
        doAnswer(invocation -> {
            assertTrue(infrastructure.isRunning());
            events.add("consumer.stop");
            return null;
        }).when(client).stop();

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean("infrastructure", Infrastructure.class, () -> infrastructure,
                    definition -> definition.setLazyInit(true));
            context.registerBean("client", KafkaConsumerClient.class, () -> client, definition -> {
                definition.setDependsOn("infrastructure");
                definition.setDestroyMethodName("");
            });
            context.registerBean("consumers", KafkaConsumerClientLifecycle.class,
                    () -> new KafkaConsumerClientLifecycle(List.of(context.getBean("client", KafkaConsumerClient.class))),
                    definition -> definition.setDependsOn("client", "infrastructure"));
            context.refresh();
            assertTrue(context.getBean(KafkaConsumerClientLifecycle.class).isRunning());
            assertEquals(List.of("infrastructure.init", "infrastructure.start", "consumer.start"), events);
        }
        assertEquals(List.of("infrastructure.init", "infrastructure.start", "consumer.start",
                "consumer.stop", "infrastructure.stop", "infrastructure.destroy"), events);
        verify(client, times(1)).start();
        verify(client, times(1)).stop();
    }

    @Test
    public void startup_failure_rolls_back_in_reverse_order_and_preserves_cleanup_failures() {
        KafkaConsumerClient first = mock(KafkaConsumerClient.class);
        KafkaConsumerClient failing = mock(KafkaConsumerClient.class);
        KafkaConsumerClient untouched = mock(KafkaConsumerClient.class);
        IllegalStateException failure = new IllegalStateException("start");
        IllegalStateException cleanupFailure = new IllegalStateException("cleanup");
        doThrow(failure).when(failing).start();
        doThrow(cleanupFailure).doNothing().when(failing).stop();
        KafkaConsumerClientLifecycle lifecycle = new KafkaConsumerClientLifecycle(List.of(first, failing, untouched));

        assertSame(failure, assertThrows(IllegalStateException.class, lifecycle::start));
        assertFalse(lifecycle.isRunning());
        assertArrayEquals(new Throwable[]{cleanupFailure}, failure.getSuppressed());
        var order = inOrder(first, failing);
        order.verify(first).start();
        order.verify(failing).start();
        order.verify(failing).stop();
        order.verify(first).stop();
        verifyNoInteractions(untouched);
        assertThrows(IllegalStateException.class, lifecycle::start);
        lifecycle.destroy();
        verify(first, times(1)).stop();
        verify(failing, times(2)).stop();
    }

    @Test
    public void context_propagates_start_failure_and_does_not_leave_the_group_started() {
        KafkaConsumerClient first = mock(KafkaConsumerClient.class);
        KafkaConsumerClient failing = mock(KafkaConsumerClient.class);
        IllegalStateException failure = new IllegalStateException("start");
        doThrow(failure).when(failing).start();
        KafkaConsumerClientLifecycle lifecycle = new KafkaConsumerClientLifecycle(List.of(first, failing));
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean("consumers", KafkaConsumerClientLifecycle.class, () -> lifecycle);
            RuntimeException refreshFailure = assertThrows(RuntimeException.class, context::refresh);
            assertSame(failure, refreshFailure.getCause());
            assertFalse(lifecycle.isRunning());
        }
        verify(first, times(1)).stop();
        verify(failing, times(1)).stop();
    }

    @Test
    public void refresh_cancellation_after_start_destroys_the_lifecycle_owner() {
        KafkaConsumerClient client = mock(KafkaConsumerClient.class);
        KafkaConsumerClientLifecycle lifecycle = new KafkaConsumerClientLifecycle(List.of(client));
        IllegalStateException failure = new IllegalStateException("refresh cancelled");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean("consumers", KafkaConsumerClientLifecycle.class, () -> lifecycle);
            context.addApplicationListener(event -> {
                if (event instanceof ContextRefreshedEvent) {
                    throw failure;
                }
            });
            assertSame(failure, assertThrows(IllegalStateException.class, context::refresh));
            assertFalse(lifecycle.isRunning());
        }
        verify(client, times(1)).start();
        verify(client, times(1)).stop();
    }

    @Test
    public void repeated_start_stop_and_destroy_are_safe_and_callback_follows_completion() {
        KafkaConsumerClient client = mock(KafkaConsumerClient.class);
        KafkaConsumerClientLifecycle lifecycle = new KafkaConsumerClientLifecycle(List.of(client), 123);
        AtomicInteger callbacks = new AtomicInteger();
        lifecycle.stop(callbacks::incrementAndGet);
        lifecycle.start();
        lifecycle.start();
        lifecycle.stop(() -> {
            verify(client).stop();
            assertFalse(lifecycle.isRunning());
            callbacks.incrementAndGet();
        });
        lifecycle.stop(callbacks::incrementAndGet);
        lifecycle.destroy();
        assertEquals(3, callbacks.get());
        assertEquals(123, lifecycle.getPhase());
        verify(client, times(1)).start();
        verify(client, times(1)).stop();
        lifecycle.start();
        lifecycle.stop();
        verify(client, times(2)).start();
        verify(client, times(2)).stop();
    }

    @Test
    public void failed_stop_attempts_every_client_and_does_not_acknowledge_completion() {
        KafkaConsumerClient first = mock(KafkaConsumerClient.class);
        KafkaConsumerClient second = mock(KafkaConsumerClient.class);
        IllegalStateException firstFailure = new IllegalStateException("first");
        IllegalStateException secondFailure = new IllegalStateException("second");
        doThrow(firstFailure).doNothing().when(first).stop();
        doThrow(secondFailure).doNothing().when(second).stop();
        KafkaConsumerClientLifecycle lifecycle = new KafkaConsumerClientLifecycle(List.of(first, second));
        AtomicInteger callbacks = new AtomicInteger();
        lifecycle.start();
        assertSame(secondFailure, assertThrows(IllegalStateException.class,
                () -> lifecycle.stop(callbacks::incrementAndGet)));
        assertArrayEquals(new Throwable[]{firstFailure}, secondFailure.getSuppressed());
        assertEquals(0, callbacks.get());
        lifecycle.stop(callbacks::incrementAndGet);
        assertEquals(1, callbacks.get());
    }

    @Test
    public void disabled_consumer_is_lifecycle_running_and_can_resume_without_another_lifecycle_start() {
        KafkaConsumerClient client = mock(KafkaConsumerClient.class);
        AtomicBoolean polling = new AtomicBoolean();
        AtomicBoolean disabled = new AtomicBoolean(true);
        when(client.isRunning()).thenAnswer(invocation -> polling.get());
        doAnswer(invocation -> { polling.set(true); return null; }).when(client).start();
        doAnswer(invocation -> { polling.set(false); return null; }).when(client).stop();
        FeatureToggledKafkaConsumerClient toggled = new FeatureToggledKafkaConsumerClient(
                client, disabled::get, Duration.ofMillis(10));
        KafkaConsumerClientLifecycle lifecycle = new KafkaConsumerClientLifecycle(List.of(toggled));
        try {
            lifecycle.start();
            lifecycle.start();
            assertTrue(lifecycle.isRunning());
            assertFalse(toggled.isRunning());
            verify(client, never()).start();
            disabled.set(false);
            await().atMost(Duration.ofSeconds(2)).until(polling::get);
            disabled.set(true);
            await().atMost(Duration.ofSeconds(2)).until(() -> !polling.get());
            assertTrue(lifecycle.isRunning());
        } finally {
            lifecycle.stop();
        }
        assertFalse(lifecycle.isRunning());
        verify(client, times(1)).start();
    }

    @Test
    public void shutdown_during_start_waits_and_does_not_duplicate_workers() throws Exception {
        KafkaConsumerClient client = mock(KafkaConsumerClient.class);
        CountDownLatch starting = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            starting.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return null;
        }).when(client).start();
        KafkaConsumerClientLifecycle lifecycle = new KafkaConsumerClientLifecycle(List.of(client));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> start = executor.submit(lifecycle::start);
            assertTrue(starting.await(5, TimeUnit.SECONDS));
            AtomicInteger callbacks = new AtomicInteger();
            CountDownLatch stopping = new CountDownLatch(1);
            Future<?> stop = executor.submit(() -> {
                stopping.countDown();
                lifecycle.stop(callbacks::incrementAndGet);
            });
            assertTrue(stopping.await(5, TimeUnit.SECONDS));
            assertFalse(stop.isDone());
            assertEquals(0, callbacks.get());
            release.countDown();
            start.get(5, TimeUnit.SECONDS);
            stop.get(5, TimeUnit.SECONDS);
            assertFalse(lifecycle.isRunning());
            assertEquals(1, callbacks.get());
            verify(client, times(1)).start();
            verify(client, times(1)).stop();
        } finally {
            release.countDown();
            executor.shutdownNow();
            lifecycle.stop();
        }
    }

    @Test
    public void concurrent_starts_only_start_the_client_once() throws Exception {
        KafkaConsumerClient client = mock(KafkaConsumerClient.class);
        CountDownLatch starting = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            starting.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return null;
        }).when(client).start();
        KafkaConsumerClientLifecycle lifecycle = new KafkaConsumerClientLifecycle(List.of(client));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(lifecycle::start);
            assertTrue(starting.await(5, TimeUnit.SECONDS));
            CountDownLatch secondStarting = new CountDownLatch(1);
            Future<?> second = executor.submit(() -> {
                secondStarting.countDown();
                lifecycle.start();
            });
            assertTrue(secondStarting.await(5, TimeUnit.SECONDS));
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertTrue(lifecycle.isRunning());
            verify(client, times(1)).start();
        } finally {
            release.countDown();
            executor.shutdownNow();
            lifecycle.stop();
        }
    }

    @Test
    public void duplicate_consumers_are_rejected_before_startup() {
        KafkaConsumerClient client = mock(KafkaConsumerClient.class);
        assertThrows(IllegalArgumentException.class,
                () -> new KafkaConsumerClientLifecycle(List.of(client, client)));
        verifyNoInteractions(client);
    }

    private static class Infrastructure implements SmartLifecycle, InitializingBean, DisposableBean {
        private final List<String> events;
        private final int phase;
        private boolean initialized;
        private boolean running;

        private Infrastructure(List<String> events, int phase) {
            this.events = events;
            this.phase = phase;
        }

        @Override
        public void afterPropertiesSet() {
            initialized = true;
            events.add("infrastructure.init");
        }

        @Override
        public void start() {
            running = true;
            events.add("infrastructure.start");
        }

        @Override
        public void stop() {
            running = false;
            events.add("infrastructure.stop");
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public int getPhase() {
            return phase;
        }

        @Override
        public void destroy() {
            events.add("infrastructure.destroy");
        }
    }
}
