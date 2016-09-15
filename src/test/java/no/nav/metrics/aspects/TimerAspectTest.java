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
            aspect.timerPaMetode((ProceedingJoinPoint) any, (Timed) any); result = "proxyTimed";
        }};

        TimedMetoder proxy = lagAspectProxy(new TimedMetoder(), aspect);

        assertEquals("proxyTimed", proxy.timed());
        assertEquals("originalIkkeTimed", proxy.ikkeTimed());
    }


    @Test
    public void timeMetodeBlirKaltMedRiktigNavn(@Mocked final MetodeTimer metodeTimer) throws Throwable {
        new Expectations() {{
            MetodeTimer.timeMetode((Metodekall) any, anyString); result = "timedMetode";
        }};

        TimerAspect aspect = new TimerAspect();

        TimedMetoder timedMetoder = lagAspectProxy(new TimedMetoder(), aspect);
        timedMetoder.timed();
        timedMetoder.timedMedNavn();

        new Verifications() {{
            MetodeTimer.timeMetode((Metodekall) any, "TimedMetoder.timed");
            MetodeTimer.timeMetode((Metodekall) any, "customTimerNavn");
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


}
