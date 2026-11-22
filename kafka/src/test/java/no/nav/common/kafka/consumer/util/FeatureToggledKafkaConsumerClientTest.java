package no.nav.common.kafka.consumer.util;

import static no.nav.common.kafka.util.WorkerLifecycleTest.avbrytForsteArbeidsoppgave;
import static no.nav.common.kafka.util.WorkerLifecycleTest.ryddExecutor;
import static no.nav.common.kafka.util.WorkerLifecycleTest.vent;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.util.WorkerLifecycle;
import no.nav.common.kafka.util.WorkerLifecycle.CloseTimeoutException;
import org.junit.Test;

public class FeatureToggledKafkaConsumerClientTest {

    @Test
    public void skal_tillate_stopp_omstart_og_lukking_gjennom_nestede_wrappere() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        AtomicBoolean running = new AtomicBoolean();
        when(underlying.isRunning()).thenAnswer(invocation -> running.get());
        doAnswer(invocation -> {
                    running.set(true);
                    return null;
                })
                .when(underlying)
                .start();
        doAnswer(invocation -> {
                    running.set(false);
                    return null;
                })
                .when(underlying)
                .stop();
        FeatureToggledKafkaConsumerClient inner =
                new FeatureToggledKafkaConsumerClient(underlying, () -> false, Duration.ofDays(1));
        FeatureToggledKafkaConsumerClient outer =
                new FeatureToggledKafkaConsumerClient(inner, () -> false, Duration.ofDays(1));
        try {
            outer.start();
            assertTrue(outer.isRunning());
            outer.stop();
            assertFalse(outer.isRunning());
            outer.start();
            assertTrue(outer.isRunning());
            outer.close(Duration.ofSeconds(3));
            assertFalse(outer.isRunning());
            verify(underlying, times(2)).start();
            verify(underlying, times(2)).stop();
            verify(underlying).close();
        } finally {
            outer.close(Duration.ofSeconds(3));
        }
    }

    @Test
    public void skal_avvise_venting_pa_forelder_under_delegert_initialisering() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        FeatureToggledKafkaConsumerClient[] holder = new FeatureToggledKafkaConsumerClient[1];
        AtomicInteger rejected = new AtomicInteger();
        FeatureToggledKafkaConsumerClient inner = new FeatureToggledKafkaConsumerClient(
                underlying,
                () -> {
                    assertThrows(IllegalStateException.class, holder[0]::stop);
                    rejected.incrementAndGet();
                    return false;
                },
                Duration.ofDays(1));
        holder[0] = new FeatureToggledKafkaConsumerClient(inner, () -> false, Duration.ofDays(1));
        try {
            holder[0].start();
            holder[0].stop();
            holder[0].close(Duration.ofSeconds(3));
            assertEquals(1, rejected.get());
            verify(underlying).close();
        } finally {
            holder[0].close(Duration.ofSeconds(3));
        }
    }

    @Test
    public void skal_fortsette_underliggende_lukking_etter_klientens_ventefrist() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        CountDownLatch confirming = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doThrow(new CloseTimeoutException("Simulert underliggende ventefrist"))
                .doAnswer(invocation -> {
                    confirming.countDown();
                    vent(release);
                    return null;
                })
                .when(underlying)
                .close();
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> false);
        try {
            assertThrows(CloseTimeoutException.class, () -> client.close(Duration.ZERO));
            vent(confirming);
            assertThrows(CloseTimeoutException.class, () -> client.close(Duration.ZERO));
            release.countDown();
            client.close(Duration.ofSeconds(3));
            client.close(Duration.ofSeconds(3));
            verify(underlying, times(2)).close();
        } finally {
            release.countDown();
            client.close(Duration.ofSeconds(3));
        }
    }

    @Test
    public void skal_bevare_faktisk_lukkefeil_uten_a_forsoke_underliggende_close_igjen() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        IllegalStateException failure = new IllegalStateException(
                "Simulert ressursfeil", new CloseTimeoutException("Feil fra en ressurs, ikke kallerens venting"));
        doThrow(failure).when(underlying).close();
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> false);
        assertEquals(
                failure,
                assertThrows(IllegalStateException.class, () -> client.close(Duration.ofSeconds(3)))
                        .getCause());
        assertEquals(
                failure,
                assertThrows(IllegalStateException.class, () -> client.close(Duration.ofSeconds(3)))
                        .getCause());
        verify(underlying).close();
    }

    @Test
    public void skal_bevare_error_fra_toggle_og_lukke_etter_mislykket_oppstart() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        AssertionError failure = new AssertionError("Simulert toggle-feil");
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> {
            throw failure;
        });
        try {
            assertEquals(failure, assertThrows(AssertionError.class, client::start));
            client.close(Duration.ofSeconds(3));
            verify(underlying, never()).start();
            verify(underlying).stop();
            verify(underlying).close();
        } finally {
            client.close(Duration.ofSeconds(3));
        }
    }

    @Test
    public void skal_logge_avbrutt_vanlig_stopp_mens_toggle_arbeidet_fullfores() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        CountDownLatch polling = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        FeatureToggledKafkaConsumerClient client = clientMedBlokkertToggle(underlying, polling, release);
        try {
            client.start();
            vent(polling);
            Thread.currentThread().interrupt();
            try {
                client.stop();
            } finally {
                assertTrue("Avbrutt venting skal gjenopprette interrupt-statusen", Thread.interrupted());
            }
            assertThrows(IllegalStateException.class, () -> client.close(Duration.ZERO));
            release.countDown();
            client.close(Duration.ofSeconds(3));
            verify(underlying).stop();
        } finally {
            release.countDown();
            client.close(Duration.ofSeconds(3));
        }
    }

    @Test
    public void skal_stoppe_toggle_kjoringen_og_gjenopprette_interrupt_etter_initialisering() throws Exception {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        AtomicBoolean interruptedDuringStop = new AtomicBoolean();
        doAnswer(invocation -> {
                    interruptedDuringStop.set(Thread.currentThread().isInterrupted());
                    return null;
                })
                .when(underlying)
                .stop();
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> false);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean interruptedAfterWork = new AtomicBoolean();
        try {
            WorkerLifecycle lifecycle = avbrytForsteArbeidsoppgave(client, completed, interruptedAfterWork);
            client.start();
            vent(completed);
            assertFalse(lifecycle.isRunning());
            assertTrue(interruptedAfterWork.get());
            assertFalse(interruptedDuringStop.get());
            verify(underlying).start();
            verify(underlying).stop();
        } finally {
            client.close(Duration.ofSeconds(3));
        }
    }

    @Test
    public void skal_melde_underliggende_stoppfeil_selv_om_toggle_arbeideren_er_ferdig() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        IllegalStateException failure = new IllegalStateException("Simulert stoppfrist");
        doThrow(failure).when(underlying).stop();
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> false);
        try {
            client.start();
            assertEquals(
                    failure,
                    assertThrows(IllegalStateException.class, client::stop).getCause());
            client.close(Duration.ofSeconds(3));
        } finally {
            client.close(Duration.ofSeconds(3));
        }
    }

    @Test
    public void skal_lukke_underliggende_client_en_gang_for_oppstart() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> false);
        client.close();
        client.close();
        verify(underlying).close();
        assertThrows(IllegalStateException.class, client::start);
    }

    @Test
    public void skal_vente_pa_toggle_uten_a_starte_stoppet_eller_lukket_client() throws Exception {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        CountDownLatch polling = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger polls = new AtomicInteger();
        ExecutorService callers = Executors.newSingleThreadExecutor();
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(
                underlying,
                () -> {
                    polls.incrementAndGet();
                    polling.countDown();
                    vent(release);
                    return false;
                },
                Duration.ofDays(1));
        Throwable failure = null;
        try {
            Future<?> start = callers.submit(client::start);
            vent(polling);
            client.start();
            assertThrows(IllegalStateException.class, () -> client.close(Duration.ZERO));
            release.countDown();
            start.get(3, TimeUnit.SECONDS);
            client.close(Duration.ofSeconds(3));
            assertEquals(1, polls.get());
            verify(underlying, never()).start();
            verify(underlying).stop();
            verify(underlying).close();
        } catch (Throwable e) {
            failure = e;
            throw e;
        } finally {
            ryddExecutor(callers, failure, () -> {
                release.countDown();
                client.close(Duration.ofSeconds(3));
            });
        }
    }

    @Test
    public void skal_vente_pa_underliggende_lukking_og_melde_tidsavbrudd() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        CountDownLatch closing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
                    closing.countDown();
                    vent(release);
                    return null;
                })
                .when(underlying)
                .close();
        FeatureToggledKafkaConsumerClient client = new FeatureToggledKafkaConsumerClient(underlying, () -> false);
        try {
            assertThrows(IllegalStateException.class, () -> client.close(Duration.ZERO));
            vent(closing);
            assertThrows(IllegalStateException.class, () -> client.close(Duration.ZERO));
            release.countDown();
            client.close(Duration.ofSeconds(3));
            verify(underlying).close();
        } finally {
            release.countDown();
            client.close(Duration.ofSeconds(3));
        }
    }

    @Test
    public void skal_avvise_selvventing_fra_toggle_og_fullfore_lukking() {
        KafkaConsumerClient underlying = mock(KafkaConsumerClient.class);
        CountDownLatch returned = new CountDownLatch(1);
        FeatureToggledKafkaConsumerClient[] holder = new FeatureToggledKafkaConsumerClient[1];
        holder[0] = new FeatureToggledKafkaConsumerClient(underlying, () -> {
            assertThrows(IllegalStateException.class, () -> holder[0].close(Duration.ofSeconds(3)));
            returned.countDown();
            return false;
        });
        try {
            holder[0].start();
            vent(returned);
            holder[0].close(Duration.ofSeconds(3));
            verify(underlying, never()).start();
            verify(underlying).close();
        } finally {
            holder[0].close(Duration.ofSeconds(3));
        }
    }

    private static FeatureToggledKafkaConsumerClient clientMedBlokkertToggle(
            KafkaConsumerClient underlying, CountDownLatch polling, CountDownLatch release) {
        AtomicInteger polls = new AtomicInteger();
        return new FeatureToggledKafkaConsumerClient(
                underlying,
                () -> {
                    if (polls.incrementAndGet() > 1) {
                        polling.countDown();
                        vent(release);
                        assertFalse(Thread.currentThread().isInterrupted());
                    }
                    return false;
                },
                Duration.ofMillis(1));
    }

    @Test
    public void should_start_consumer_when_toggle_is_off_and_consumer_is_not_running() {
        KafkaConsumerClient consumerClient = mock(KafkaConsumerClient.class);
        AtomicBoolean toggleForStoppingConsumers = new AtomicBoolean(true);

        when(consumerClient.isRunning()).thenReturn(false);

        try (FeatureToggledKafkaConsumerClient toggledClient = new FeatureToggledKafkaConsumerClient(
                consumerClient, toggleForStoppingConsumers::get, Duration.ofMillis(10))) {
            toggledClient.start();
            toggleForStoppingConsumers.set(false);
            await().atMost(Duration.ofSeconds(1))
                    .untilAsserted(() -> verify(consumerClient, atLeastOnce()).start());
        }
    }

    @Test
    public void should_stop_consumer_when_toggle_is_on_and_consumer_is_running() {
        KafkaConsumerClient consumerClient = mock(KafkaConsumerClient.class);
        AtomicBoolean toggleForStoppingConsumers = new AtomicBoolean(false);
        AtomicBoolean running = new AtomicBoolean(false);

        when(consumerClient.isRunning()).thenAnswer(invocation -> running.get());

        try (FeatureToggledKafkaConsumerClient toggledClient = new FeatureToggledKafkaConsumerClient(
                consumerClient, toggleForStoppingConsumers::get, Duration.ofMillis(10))) {
            toggledClient.start();
            running.set(true);
            toggleForStoppingConsumers.set(true);
            await().atMost(Duration.ofSeconds(1))
                    .untilAsserted(() -> verify(consumerClient, atLeastOnce()).stop());
        }
    }

    @Test
    public void should_not_stop_consumer_when_toggle_is_off_and_consumer_is_running() {
        KafkaConsumerClient consumerClient = mock(KafkaConsumerClient.class);
        AtomicBoolean toggleForStoppingConsumers = new AtomicBoolean(true);
        AtomicInteger pollsWithToggleOff = new AtomicInteger();
        AtomicBoolean running = new AtomicBoolean(false);

        when(consumerClient.isRunning()).thenAnswer(invocation -> running.get());

        try (FeatureToggledKafkaConsumerClient toggledClient = new FeatureToggledKafkaConsumerClient(
                consumerClient,
                () -> {
                    boolean toggledOff = toggleForStoppingConsumers.get();
                    if (!toggledOff) {
                        pollsWithToggleOff.incrementAndGet();
                    }
                    return toggledOff;
                },
                Duration.ofMillis(10))) {
            toggledClient.start();
            toggleForStoppingConsumers.set(false);
            await().atMost(Duration.ofSeconds(1)).until(() -> pollsWithToggleOff.get() > 0);
            running.set(true);
            int pollsBeforeRunning = pollsWithToggleOff.get();
            await().atMost(Duration.ofSeconds(1)).until(() -> pollsWithToggleOff.get() > pollsBeforeRunning);
            await().during(Duration.ofMillis(50))
                    .atMost(Duration.ofSeconds(1))
                    .untilAsserted(() -> verify(consumerClient, never()).stop());
        }
    }
}
