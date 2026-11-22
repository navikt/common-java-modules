package no.nav.common.kafka.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class WorkerLifecycleTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    @Test
    public void skal_logge_stoppfrist_uten_a_avbryte_arbeidet_eller_hindre_omstart() {
        WorkerLifecycle lifecycle = new WorkerLifecycle(1, () -> {}, false);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        try {
            lifecycle.start(() -> {
                entered.countDown();
                vent(release);
                interrupted.set(Thread.currentThread().isInterrupted());
            });
            vent(entered);
            lifecycle.stopAndAwaitBestEffort(Duration.ZERO, () -> {}, null);
            assertFalse(lifecycle.isRunning());
            assertThrows(IllegalStateException.class, () -> lifecycle.stopAndAwait(Duration.ZERO, () -> {}, null));
            release.countDown();
            lifecycle.stopAndAwait(TIMEOUT, () -> {}, null);
            assertFalse(interrupted.get());
            CountDownLatch restarted = new CountDownLatch(1);
            lifecycle.start(restarted::countDown);
            vent(restarted);
            lifecycle.stopAndAwaitBestEffort(TIMEOUT, () -> {}, null);
        } finally {
            release.countDown();
            lifecycle.close(TIMEOUT);
        }
    }

    @Test
    public void skal_logge_avbrutt_best_effort_stopp_og_gjenopprette_interrupt() {
        WorkerLifecycle lifecycle = new WorkerLifecycle(1, () -> {}, false);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            lifecycle.start(() -> {
                entered.countDown();
                vent(release);
                assertFalse(Thread.currentThread().isInterrupted());
            });
            vent(entered);
            Thread.currentThread().interrupt();
            try {
                lifecycle.stopAndAwaitBestEffort(TIMEOUT, () -> {}, null);
            } finally {
                assertTrue(Thread.interrupted(), "Avbrutt venting skal gjenopprette interrupt-statusen");
            }
            assertFalse(lifecycle.isRunning());
        } finally {
            release.countDown();
            lifecycle.close(TIMEOUT);
        }
    }

    @Test
    public void skal_skille_mellom_utlopt_pause_og_stoppsignal() {
        WorkerLifecycle lifecycle = new WorkerLifecycle(1, () -> {}, false);
        CountDownLatch waiting = new CountDownLatch(1);
        AtomicBoolean stopped = new AtomicBoolean();
        try {
            assertFalse(lifecycle.pause(Duration.ZERO));
            lifecycle.start(() -> {
                try {
                    assertFalse(lifecycle.pause(Duration.ZERO));
                    waiting.countDown();
                    stopped.set(lifecycle.pause(Duration.ofDays(1)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            });
            vent(waiting);
            lifecycle.stopAndAwait(TIMEOUT, () -> {}, null);
            assertTrue(stopped.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        } finally {
            lifecycle.close(TIMEOUT);
        }
    }

    @Test
    public void skal_bevare_begge_feil_nar_to_oppgaver_feiler_samtidig() {
        WorkerLifecycle lifecycle = new WorkerLifecycle(2, () -> {}, false);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        RuntimeException first = new IllegalStateException("Simulert første feil");
        RuntimeException second = new IllegalStateException("Simulert andre feil");
        try {
            lifecycle.start(
                    () -> {
                        ready.countDown();
                        vent(release);
                        throw first;
                    },
                    () -> {
                        ready.countDown();
                        vent(release);
                        throw second;
                    });
            vent(ready);
            release.countDown();
            Throwable failure = assertThrows(
                            IllegalStateException.class, () -> lifecycle.stopAndAwait(TIMEOUT, () -> {}, null))
                    .getCause();
            assertTrue(failure == first || failure == second);
            assertEquals(1, failure.getSuppressed().length);
            assertEquals(failure == first ? second : first, failure.getSuppressed()[0]);
            CountDownLatch restarted = new CountDownLatch(2);
            lifecycle.start(restarted::countDown, restarted::countDown);
            vent(restarted);
            lifecycle.stopAndAwait(TIMEOUT, () -> {}, null);
        } finally {
            release.countDown();
            lifecycle.close(TIMEOUT);
        }
    }

    @Test
    public void skal_lagre_error_for_ferdigmelding_og_sende_den_videre_fra_arbeideren() {
        WorkerLifecycle lifecycle = new WorkerLifecycle(1, () -> {}, false);
        AssertionError failure = new AssertionError("Simulert arbeiderfeil");
        CountDownLatch reported = new CountDownLatch(1);
        AtomicReference<Throwable> uncaught = new AtomicReference<>();
        try {
            lifecycle.start(() -> {
                Thread.currentThread().setUncaughtExceptionHandler((thread, exception) -> {
                    uncaught.set(exception);
                    reported.countDown();
                });
                throw failure;
            });
            assertEquals(
                    failure,
                    assertThrows(IllegalStateException.class, () -> lifecycle.stopAndAwait(TIMEOUT, () -> {}, null))
                            .getCause());
            vent(reported);
            assertEquals(failure, uncaught.get());
            assertFalse(lifecycle.isRunning());
        } finally {
            lifecycle.close(TIMEOUT);
        }
    }

    @Test
    public void skal_vente_pa_initialisering_etter_gjentatte_avbrudd_og_se_publiserte_verdier() throws Exception {
        ExecutorService caller = Executors.newSingleThreadExecutor();
        CountDownLatch waiting = new CountDownLatch(1);
        int[] initializedValue = new int[1];
        CountDownLatch initialized = new CountDownLatch(1) {
            private int attempts;

            @Override
            public void await() throws InterruptedException {
                if (attempts++ < 2) {
                    Thread.currentThread().interrupt();
                } else {
                    waiting.countDown();
                }
                super.await();
            }
        };
        Throwable failure = null;
        try {
            Future<int[]> result = caller.submit(() -> {
                boolean interrupted = WorkerLifecycle.awaitInitializationAndReportInterruption(initialized);
                return new int[] {
                    interrupted ? 1 : 0, Thread.currentThread().isInterrupted() ? 1 : 0, initializedValue[0]
                };
            });
            vent(waiting);
            assertFalse(result.isDone());
            initializedValue[0] = 42;
            initialized.countDown();
            int[] values = result.get(3, TimeUnit.SECONDS);
            assertEquals(1, values[0]);
            assertEquals(0, values[1]);
            assertEquals(42, values[2]);
        } catch (Throwable e) {
            failure = e;
            throw e;
        } finally {
            ryddExecutor(caller, failure, initialized::countDown);
        }
    }

    @Test
    public void skal_rapportere_tidligere_interrupt_uten_a_gjenopprette_statusen_i_hjelperen() {
        try {
            assertFalse(WorkerLifecycle.awaitInitializationAndReportInterruption(new CountDownLatch(0)));
            Thread.currentThread().interrupt();
            assertTrue(WorkerLifecycle.awaitInitializationAndReportInterruption(new CountDownLatch(0)));
            assertFalse(Thread.currentThread().isInterrupted());
            Thread.currentThread().interrupt();
            assertTrue(Thread.interrupted());
        } finally {
            assertFalse(Thread.interrupted(), "Testtråden hadde en uventet interrupt-status");
        }
    }

    @Test
    public void skal_vente_pa_bekreftet_executor_avslutning_etter_avbrudd_og_tidsavbrudd() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        when(executor.awaitTermination(1, TimeUnit.SECONDS))
                .thenThrow(new InterruptedException())
                .thenReturn(false, true);
        try {
            Thread.currentThread().interrupt();
            assertTrue(WorkerLifecycle.awaitTerminationAndReportInterruption(executor));
            assertFalse(Thread.currentThread().isInterrupted());
            verify(executor, never()).shutdown();
            when(executor.isTerminated()).thenReturn(true);
            assertFalse(WorkerLifecycle.awaitTerminationAndReportInterruption(executor));
            Thread.currentThread().interrupt();
            assertTrue(WorkerLifecycle.awaitTerminationAndReportInterruption(executor));
            assertFalse(Thread.currentThread().isInterrupted());
            assertEquals(
                    3,
                    mockingDetails(executor).getInvocations().stream()
                            .filter(invocation ->
                                    invocation.getMethod().getName().equals("awaitTermination"))
                            .count());
        } finally {
            assertFalse(Thread.interrupted(), "Testtråden hadde en uventet interrupt-status");
        }
    }

    @Test
    public void skal_melde_arbeiderfeil_for_stoppet_kjoring_uten_a_arve_feilen_ved_omstart() {
        WorkerLifecycle lifecycle = new WorkerLifecycle(1, () -> {}, false);
        CountDownLatch entered = new CountDownLatch(1);
        IllegalStateException failure = new IllegalStateException("Simulert arbeiderfeil");
        try {
            lifecycle.start(() -> {
                entered.countDown();
                throw failure;
            });
            vent(entered);
            assertEquals(
                    failure,
                    assertThrows(IllegalStateException.class, () -> lifecycle.stopAndAwait(TIMEOUT, () -> {}, null))
                            .getCause());
            assertEquals(
                    failure,
                    assertThrows(
                                    IllegalStateException.class,
                                    () -> lifecycle.stopAndAwaitBestEffort(TIMEOUT, () -> {}, null))
                            .getCause());
            CountDownLatch restarted = new CountDownLatch(1);
            lifecycle.start(restarted::countDown);
            vent(restarted);
            lifecycle.stopAndAwait(TIMEOUT, () -> {}, null);
            lifecycle.close(TIMEOUT);
        } finally {
            lifecycle.close(TIMEOUT);
        }
    }

    @Test
    public void skal_avslutte_alle_executorene_og_fjerne_shutdown_hook() throws Exception {
        WorkerLifecycle lifecycle = new WorkerLifecycle(2, () -> {}, true);
        lifecycle.start(() -> {}, () -> {});
        lifecycle.close(TIMEOUT);
        Field executorsField = WorkerLifecycle.class.getDeclaredField("executors");
        executorsField.setAccessible(true);
        List<?> executors = (List<?>) executorsField.get(lifecycle);
        assertEquals(2, executors.size());
        for (Object executor : executors) {
            assertTrue(((ExecutorService) executor).isTerminated());
        }
        Field hookField = WorkerLifecycle.class.getDeclaredField("shutdownHook");
        hookField.setAccessible(true);
        assertFalse(Runtime.getRuntime().removeShutdownHook((Thread) hookField.get(lifecycle)));
    }

    @Test
    public void skal_handtere_samtidig_start_stopp_og_lukking() throws Exception {
        ExecutorService callers = Executors.newFixedThreadPool(3);
        Throwable failure = null;
        try {
            for (int i = 0; i < 20; i++) {
                AtomicInteger closed = new AtomicInteger();
                WorkerLifecycle lifecycle = new WorkerLifecycle(2, closed::incrementAndGet, false);
                CountDownLatch begin = new CountDownLatch(1);
                Runnable work = () -> {
                    try {
                        while (lifecycle.isRunning()) {
                            lifecycle.pause(Duration.ofDays(1));
                        }
                    } catch (InterruptedException e) {
                        throw new AssertionError(e);
                    }
                };
                Future<?> start = callers.submit(() -> {
                    vent(begin);
                    try {
                        lifecycle.start(work, work);
                    } catch (IllegalStateException expected) {
                        assertFalse(lifecycle.isRunning());
                    }
                });
                Future<?> stop = callers.submit(() -> {
                    vent(begin);
                    lifecycle.stopAndAwait(TIMEOUT, () -> {}, null);
                });
                Future<?> close = callers.submit(() -> {
                    vent(begin);
                    lifecycle.close(TIMEOUT);
                });
                begin.countDown();
                start.get(3, TimeUnit.SECONDS);
                stop.get(3, TimeUnit.SECONDS);
                close.get(3, TimeUnit.SECONDS);
                lifecycle.close(TIMEOUT);
                assertEquals(1, closed.get());
            }
        } catch (Throwable e) {
            failure = e;
            throw e;
        } finally {
            ryddExecutor(callers, failure, () -> {});
        }
    }

    @Test
    public void skal_lukke_for_oppstart_og_avvise_ny_oppstart() {
        AtomicInteger closed = new AtomicInteger();
        WorkerLifecycle lifecycle = new WorkerLifecycle(2, closed::incrementAndGet, true);
        lifecycle.close(TIMEOUT);
        lifecycle.close(TIMEOUT);
        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, () -> lifecycle.start(() -> {}));
    }

    @Test
    public void skal_ha_en_arbeider_ved_samtidig_oppstart_og_vente_for_omstart() throws Exception {
        WorkerLifecycle lifecycle = new WorkerLifecycle(1, () -> {}, false);
        ExecutorService callers = Executors.newFixedThreadPool(4);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        Runnable work = () -> {
            maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
            entered.countDown();
            vent(release);
            active.decrementAndGet();
        };
        Throwable failure = null;
        try {
            List<Future<?>> starts = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                starts.add(callers.submit(() -> lifecycle.start(work)));
            }
            for (Future<?> start : starts) {
                start.get(3, TimeUnit.SECONDS);
            }
            vent(entered);
            assertEquals(1, active.get());
            lifecycle.stop();
            CountDownLatch restarted = new CountDownLatch(1);
            Future<?> restart = callers.submit(() -> lifecycle.start(restarted::countDown));
            assertFalse(restart.isDone());
            release.countDown();
            restart.get(3, TimeUnit.SECONDS);
            vent(restarted);
            lifecycle.close(TIMEOUT);
            assertEquals(1, maximum.get());
        } catch (Throwable e) {
            failure = e;
            throw e;
        } finally {
            ryddExecutor(callers, failure, () -> {
                release.countDown();
                lifecycle.close(TIMEOUT);
            });
        }
    }

    @Test
    public void skal_ikke_gjenopplive_arbeid_etter_umiddelbar_stopp_eller_lukking() {
        for (int i = 0; i < 50; i++) {
            WorkerLifecycle lifecycle = new WorkerLifecycle(2, () -> {}, false);
            AtomicInteger finished = new AtomicInteger();
            Runnable work = () -> {
                try {
                    while (lifecycle.isRunning()) {
                        lifecycle.pause(Duration.ofDays(1));
                    }
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                } finally {
                    finished.incrementAndGet();
                }
            };
            lifecycle.start(work, work);
            lifecycle.stop();
            lifecycle.close(TIMEOUT);
            assertEquals(2, finished.get());
            assertFalse(lifecycle.isRunning());
        }
    }

    @Test
    public void skal_melde_tidsavbrudd_uten_a_avbryte_arbeid_og_vente_pa_ressurslukking() {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch closing = new CountDownLatch(1);
        CountDownLatch closeRelease = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        WorkerLifecycle lifecycle = new WorkerLifecycle(
                1,
                () -> {
                    closing.countDown();
                    vent(closeRelease);
                },
                false);
        try {
            lifecycle.start(() -> {
                entered.countDown();
                vent(release);
                interrupted.set(Thread.currentThread().isInterrupted());
            });
            vent(entered);
            assertThrows(IllegalStateException.class, () -> lifecycle.close(Duration.ZERO));
            assertThrows(IllegalStateException.class, () -> lifecycle.start(() -> {}));
            release.countDown();
            vent(closing);
            assertThrows(IllegalStateException.class, () -> lifecycle.close(Duration.ZERO));
            assertFalse(interrupted.get());
        } finally {
            release.countDown();
            closeRelease.countDown();
            lifecycle.close(TIMEOUT);
        }
    }

    @Test
    public void skal_gjenopprette_interrupt_og_melde_avbrutt_venting() {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        WorkerLifecycle lifecycle = new WorkerLifecycle(1, () -> {}, false);
        try {
            lifecycle.start(() -> {
                entered.countDown();
                vent(release);
            });
            vent(entered);
            Thread.currentThread().interrupt();
            try {
                IllegalStateException failure =
                        assertThrows(IllegalStateException.class, () -> lifecycle.close(TIMEOUT));
                assertTrue(failure.getCause() instanceof InterruptedException);
            } finally {
                assertTrue(Thread.interrupted(), "Avbrutt venting skal gjenopprette interrupt-statusen");
            }
        } finally {
            release.countDown();
            lifecycle.close(TIMEOUT);
        }
    }

    @Test
    public void skal_melde_avbrutt_stopp_og_omstart_uten_a_avbryte_arbeideren() {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        WorkerLifecycle lifecycle = new WorkerLifecycle(1, () -> {}, false);
        try {
            lifecycle.start(() -> {
                entered.countDown();
                vent(release);
            });
            vent(entered);
            lifecycle.stop();
            Thread.currentThread().interrupt();
            assertThrows(IllegalStateException.class, () -> lifecycle.stopAndAwait(TIMEOUT, () -> {}, null));
            assertTrue(Thread.interrupted());
            Thread.currentThread().interrupt();
            assertThrows(IllegalStateException.class, () -> lifecycle.start(() -> {}));
            assertTrue(Thread.interrupted());
        } finally {
            release.countDown();
            lifecycle.close(TIMEOUT);
        }
    }

    @Test
    public void skal_avvise_selvventing_men_fullfore_lukking() {
        CountDownLatch returned = new CountDownLatch(1);
        AtomicInteger closed = new AtomicInteger();
        WorkerLifecycle lifecycle = new WorkerLifecycle(1, closed::incrementAndGet, false);
        lifecycle.start(() -> {
            try {
                assertThrows(
                        IllegalStateException.class, () -> lifecycle.stopAndAwaitBestEffort(TIMEOUT, () -> {}, null));
                assertThrows(IllegalStateException.class, () -> lifecycle.close(TIMEOUT));
            } finally {
                returned.countDown();
            }
        });
        vent(returned);
        lifecycle.close(TIMEOUT);
        assertEquals(1, closed.get());
    }

    @Test
    public void skal_melde_ressursfeil_ved_hver_lukking() {
        RuntimeException failure = new IllegalStateException("Simulert lukkefeil");
        WorkerLifecycle lifecycle = new WorkerLifecycle(
                1,
                () -> {
                    throw failure;
                },
                false);
        assertEquals(
                failure,
                assertThrows(IllegalStateException.class, () -> lifecycle.close(TIMEOUT))
                        .getCause());
        assertEquals(
                failure,
                assertThrows(IllegalStateException.class, () -> lifecycle.close(TIMEOUT))
                        .getCause());
    }

    @Test
    public void skal_melde_error_fra_ressurslukking_ved_hver_bekreftet_lukking() {
        AssertionError failure = new AssertionError("Simulert ressursfeil");
        WorkerLifecycle lifecycle = new WorkerLifecycle(
                1,
                () -> {
                    throw failure;
                },
                false);
        assertEquals(
                failure,
                assertThrows(IllegalStateException.class, () -> lifecycle.close(TIMEOUT))
                        .getCause());
        assertEquals(
                failure,
                assertThrows(IllegalStateException.class, () -> lifecycle.close(TIMEOUT))
                        .getCause());
    }

    @Test
    public void skal_melde_nullpekerfeil_fra_manglende_ressurslukking() {
        WorkerLifecycle lifecycle = new WorkerLifecycle(1, null, false);
        assertTrue(
                assertThrows(IllegalStateException.class, () -> lifecycle.close(TIMEOUT))
                                .getCause()
                        instanceof NullPointerException);
    }

    @Test
    public void skal_lukke_ressurser_etter_error_fra_stoppsignal_og_bevare_begge_feil() {
        AssertionError signalFailure = new AssertionError("Simulert signalfeil");
        AssertionError closeFailure = new AssertionError("Simulert lukkefeil");
        AtomicInteger closed = new AtomicInteger();
        WorkerLifecycle lifecycle = new WorkerLifecycle(
                1,
                () -> {
                    closed.incrementAndGet();
                    throw closeFailure;
                },
                false);
        assertEquals(
                signalFailure,
                assertThrows(
                                IllegalStateException.class,
                                () -> lifecycle.close(TIMEOUT, () -> {
                                    throw signalFailure;
                                }))
                        .getCause());
        assertEquals(1, closed.get());
        assertEquals(1, signalFailure.getSuppressed().length);
        assertEquals(closeFailure, signalFailure.getSuppressed()[0]);
        assertEquals(
                signalFailure,
                assertThrows(IllegalStateException.class, () -> lifecycle.close(TIMEOUT))
                        .getCause());
        assertThrows(IllegalStateException.class, () -> lifecycle.start(() -> {}));
    }

    @Test
    public void skal_bevare_original_testfeil_og_knytte_til_oppryddingsfeil() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        when(executor.awaitTermination(3, TimeUnit.SECONDS)).thenReturn(false);
        AssertionError original = new AssertionError("Simulert testfeil");
        IllegalStateException cleanupFailure = new IllegalStateException("Simulert lukkefeil");
        ryddExecutor(executor, original, () -> {
            throw cleanupFailure;
        });
        assertEquals(1, original.getSuppressed().length);
        assertEquals(cleanupFailure, original.getSuppressed()[0]);
        assertEquals(1, cleanupFailure.getSuppressed().length);
        assertTrue(cleanupFailure.getSuppressed()[0] instanceof AssertionError);
    }

    public static void ryddExecutor(ExecutorService executor, Throwable original, Runnable cleanup)
            throws InterruptedException {
        Throwable failure = null;
        try {
            cleanup.run();
        } catch (RuntimeException | Error e) {
            failure = e;
        }
        try {
            executor.shutdown();
            assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS), "Test-executor ble ikke avsluttet");
        } catch (InterruptedException | RuntimeException | Error e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (failure == null) {
                failure = e;
            } else if (failure != e) {
                failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            if (original != null) {
                if (original != failure) {
                    original.addSuppressed(failure);
                }
            } else if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            } else if (failure instanceof Error) {
                throw (Error) failure;
            } else {
                throw (InterruptedException) failure;
            }
        }
    }

    public static WorkerLifecycle avbrytForsteArbeidsoppgave(
            Object component, CountDownLatch completed, AtomicBoolean interruptedAfterWork)
            throws ReflectiveOperationException {
        Field lifecycleField = component.getClass().getDeclaredField("lifecycle");
        lifecycleField.setAccessible(true);
        WorkerLifecycle lifecycle = (WorkerLifecycle) lifecycleField.get(component);
        Field executorsField = WorkerLifecycle.class.getDeclaredField("executors");
        executorsField.setAccessible(true);
        List<?> executors = (List<?>) executorsField.get(lifecycle);
        ExecutorService executor = (ExecutorService) executors.getFirst();
        ExecutorService observed = mock(ExecutorService.class, delegatesTo(executor));
        AtomicBoolean firstTask = new AtomicBoolean(true);
        doAnswer(invocation -> {
                    Runnable task = invocation.getArgument(0);
                    boolean first = firstTask.getAndSet(false);
                    executor.execute(() -> {
                        if (first) {
                            Thread.currentThread().interrupt();
                        }
                        try {
                            task.run();
                        } finally {
                            if (first) {
                                interruptedAfterWork.set(Thread.currentThread().isInterrupted());
                                completed.countDown();
                            }
                        }
                    });
                    return null;
                })
                .when(observed)
                .execute(any(Runnable.class));
        List.class.getMethod("set", int.class, Object.class).invoke(executors, 0, observed);
        return lifecycle;
    }

    public static Thread shutdownHook(Object component) throws ReflectiveOperationException {
        Field lifecycleField = component.getClass().getDeclaredField("lifecycle");
        lifecycleField.setAccessible(true);
        WorkerLifecycle lifecycle = (WorkerLifecycle) lifecycleField.get(component);
        Field hookField = WorkerLifecycle.class.getDeclaredField("shutdownHook");
        hookField.setAccessible(true);
        return (Thread) hookField.get(lifecycle);
    }

    public static void vent(CountDownLatch latch) {
        try {
            assertTrue(latch.await(3, TimeUnit.SECONDS), "Latch ble ikke frigitt");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
