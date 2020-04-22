package no.nav.common.metrics;

import org.junit.Before;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimerTest {
    private Timer timer;
    private MetricsClient metricsClient;
    private Timing timing;

    @Before
    public void setUp() {
        metricsClient = mock(MetricsClient.class);
        timing = mock(Timing.class);
        timer = new Timer(metricsClient, "timer", timing);
    }

    @Test
    public void elapsedTimeReturnsDifferenceBetweenStartAndStopTimeInMillis() {
        when(timing.nanoTime()).thenReturn(1000000L);
        timer.start();
        when(timing.nanoTime()).thenReturn(3000000L);
        timer.stop();

        assertEquals(NANOSECONDS.toMillis(2000000), timer.getElpasedTimeInMillis());
    }

    @Test(expected = RuntimeException.class)
    public void runtimeExceptionIsThrownIfReportIsCalledAndTimerIsNotStopped() {
        timer.report();
    }

    @Test
    public void timerIsResetAfterReport(){
        when(timing.nanoTime()).thenReturn(1000000L);
        timer.start();
        when(timing.nanoTime()).thenReturn(2000000L);
        timer.stop();
        assertEquals(NANOSECONDS.toMillis(1000000), timer.getElpasedTimeInMillis());
        timer.report();
        assertThat(timer.getElpasedTimeInMillis()).isZero();
    }
}
