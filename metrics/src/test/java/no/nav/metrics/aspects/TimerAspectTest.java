package no.nav.metrics.aspects;

import no.nav.metrics.Metodekall;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;

import java.util.function.Function;

import static no.nav.metrics.TestUtil.lagAspectProxy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TimerAspectTest {

    @Test
    public void metoderMedTimedAnnotasjonBlirTruffetAvAspect() throws Throwable {

        TimerAspect aspect = new TimerAspect() {
            @Override
            public Object timerPaMetode(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
                return "proxyTimed";
            }
        };

        TimedMetoder proxy = lagAspectProxy(new TimedMetoder(), aspect);

        assertEquals("proxyTimed", proxy.timed());
        assertEquals("originalIkkeTimed", proxy.ikkeTimed());
    }

    @Test
    public void metoderPaKlasseMedAnnotasjonBlirTruffetAvAspect() throws Throwable {
        TimerAspect aspect = new TimerAspect() {
            @Override
            public Object timerPaKlasse(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
                return "proxyTimed";
            }
        };

        TimedKlasse proxy = lagAspectProxy(new TimedKlasse(), aspect);

        assertEquals("proxyTimed", proxy.timed1());
        assertEquals("proxyTimed", proxy.timed2());
    }

    @Test
    public void metoderPaKlasseMedAnnotasjonBlirIgnorert() throws Throwable {
        TimerAspect aspect = new TimerAspect() {
            @Override
            Object timeMetode(Metodekall metodekall, String timerNavn) throws Throwable {
                return "timedMetode";
            }
        };

        TimedKlasseMedIgnorerteMetoder proxy = lagAspectProxy(new TimedKlasseMedIgnorerteMetoder(), aspect);

        assertEquals("timedMetode", proxy.timed1());
        assertEquals("timedMetode", proxy.timed2());
        assertEquals("ignorert1", proxy.ignorert1());
        assertEquals("toString", proxy.toString());
    }

    @Test
    public void timeMetodeBlirKaltMedRiktigNavn() throws Throwable {
        Function func = mock(Function.class);
        TimerAspect aspect = new TimerAspect() {
            @Override
            Object timeMetode(Metodekall metodekall, String timerNavn) throws Throwable {
                return func.apply(timerNavn);
            }
        };

        TimedKlasse timedKlasse = lagAspectProxy(new TimedKlasse(), aspect);
        timedKlasse.timed1();
        timedKlasse.timed2();

        TimedMetoder timedMetoder = lagAspectProxy(new TimedMetoder(), aspect);
        timedMetoder.timed();
        timedMetoder.timedMedNavn();

        TimedKlasseMedIgnorerteMetoder ignorerteMetoder = lagAspectProxy(new TimedKlasseMedIgnorerteMetoder(), aspect);
        ignorerteMetoder.timed1();

        verify(func).apply("TimedKlasse.timed1");
        verify(func).apply("TimedKlasse.timed2");
        verify(func).apply("TimedMetoder.timed");
        verify(func).apply("customTimerNavn");
        verify(func).apply("customKlasseTimer.timed1");

    }

    private static class TimedMetoder {
        public TimedMetoder() {}

        @Timed
        public String timed() {
            return "originalTimed";
        }

        @Timed(name = "customTimerNavn")
        public String timedMedNavn() {
            return "timedMedNavn";
        }

        public String ikkeTimed() {
            return "originalIkkeTimed";
        }
    }

    @Timed
    private static class TimedKlasse {
        public TimedKlasse() {}

        public String timed1() {
            return "timed1";
        }

        public String timed2() {
            return "timed2";
        }
    }

    @Timed(ignoredMethods = "ignorert1", name = "customKlasseTimer")
    private static class TimedKlasseMedIgnorerteMetoder {
        public TimedKlasseMedIgnorerteMetoder() {}

        public String timed1() {
            return "timed1";
        }

        public String timed2() {
            return "timed2";
        }

        public String ignorert1() {
            return "ignorert1";
        }

        @Override
        public String toString() {
            return "toString";
        }
    }
}
