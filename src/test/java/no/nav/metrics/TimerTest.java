package no.nav.metrics;

import mockit.*;
import org.junit.Before;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertEquals;

public class TimerTest {
    private Timer timer;
    @Mocked
    MetricsClient metricsClient;

    @Before
    public void setUp() {
        timer = new Timer(metricsClient, "timer");
    }

    @Test
    public void startUsesNanoTime(@Mocked System mockedSystem) {
        timer.start();

        new Verifications() {
            {
                System.nanoTime();
                times = 1;
            }
        };
    }

    @Test
    public void stopUsesNanoTime(@Mocked System mockedSystem) {
        timer.stop();

        new Verifications() {
            {
                System.nanoTime();
                times = 1;
            }
        };
    }

    @Test
    public void elapsedTimeReturnsDifferenceBetweenStartAndStopTimeInMillis(@Mocked System mockedSystem) {
        new NonStrictExpectations() {
            {
                System.nanoTime();
                result = 1000000;
                result = 3000000;
            }
        };

        timer.start();
        timer.stop();

        long elpasedTimeInMillis = Deencapsulation.invoke(timer, "getElpasedTimeInMillis");

        assertEquals(NANOSECONDS.toMillis(2000000), elpasedTimeInMillis);
    }

    @Test(expected = RuntimeException.class)
    public void runtimeExceptionIsThrownIfReportIsCalledAndTimerIsNotStopped() {
        timer.report();
    }

    @Test
    public void timerIsResetAfterReport() {
        new StrictExpectations(timer) {
            {
                Deencapsulation.invoke(timer, "reset");
                times = 1;
            }
        };

        timer.start();
        timer.stop();
        timer.report();
    }
}