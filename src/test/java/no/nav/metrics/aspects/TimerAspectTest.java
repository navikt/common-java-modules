package no.nav.metrics.aspects;

import mockit.*;
import no.nav.metrics.MetodeTimer;
import no.nav.metrics.Metodekall;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;

import static no.nav.metrics.TestUtil.lagAspectProxy;
import static org.junit.Assert.assertEquals;

public class TimerAspectTest {

    @Test
    public void metoderMedTimedAnnotasjonBlirTruffetAvAspect(@Mocked final TimerAspect aspect) throws Throwable {
        new Expectations() {{
            aspect.timerPaMetode((ProceedingJoinPoint) any, (Timed) any);
            result = "proxyTimed";
        }};

        TimedMetoder proxy = lagAspectProxy(new TimedMetoder(), aspect);

        assertEquals("proxyTimed", proxy.timed());
        assertEquals("originalIkkeTimed", proxy.ikkeTimed());
    }


    @Test
    public void metoderPaKlasseMedAnnotasjonBlirTruffetAvAspect(@Mocked final TimerAspect aspect) throws Throwable {
        new Expectations() {{
            aspect.timerPaKlasse((ProceedingJoinPoint) any, (Timed) any);
            result = "proxyTimed";
        }};

        TimedKlasse proxy = lagAspectProxy(new TimedKlasse(), aspect);

        assertEquals("proxyTimed", proxy.timed1());
        assertEquals("proxyTimed", proxy.timed2());
    }

    @Test
    public void metoderPaKlasseMedAnnotasjonBlirIgnorert(@Mocked final MetodeTimer metodeTimer) throws Throwable {
        new Expectations() {{
            MetodeTimer.timeMetode((Metodekall) any, anyString);
            result = "timedMetode";
        }};

        TimedKlasseMedIgnorerteMetoder proxy = lagAspectProxy(new TimedKlasseMedIgnorerteMetoder(), new TimerAspect());

        assertEquals("timedMetode", proxy.timed1());
        assertEquals("timedMetode", proxy.timed2());
        assertEquals("ignorert1", proxy.ignorert1());
        assertEquals("toString", proxy.toString());
    }


    @Test
    public void timeMetodeBlirKaltMedRiktigNavn(@Mocked final MetodeTimer metodeTimer) throws Throwable {
        TimerAspect aspect = new TimerAspect();

        TimedKlasse timedKlasse = lagAspectProxy(new TimedKlasse(), aspect);
        timedKlasse.timed1();
        timedKlasse.timed2();

        TimedMetoder timedMetoder = lagAspectProxy(new TimedMetoder(), aspect);
        timedMetoder.timed();
        timedMetoder.timedMedNavn();

        TimedKlasseMedIgnorerteMetoder ignorerteMetoder = lagAspectProxy(new TimedKlasseMedIgnorerteMetoder(), new TimerAspect());
        ignorerteMetoder.timed1();


        new Verifications() {{
            MetodeTimer.timeMetode((Metodekall) any, "TimedKlasse.timed1");
            MetodeTimer.timeMetode((Metodekall) any, "TimedKlasse.timed2");

            MetodeTimer.timeMetode((Metodekall) any, "TimedMetoder.timed");
            MetodeTimer.timeMetode((Metodekall) any, "customTimerNavn");

            MetodeTimer.timeMetode((Metodekall) any, "customKlasseTimer.timed1");
        }};
    }


    private static class TimedMetoder {
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
        public String timed1() {
            return "timed1";
        }

        public String timed2() {
            return "timed2";
        }
    }

    @Timed(ignoredMethods = "ignorert1", name = "customKlasseTimer")
    private static class TimedKlasseMedIgnorerteMetoder {
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
