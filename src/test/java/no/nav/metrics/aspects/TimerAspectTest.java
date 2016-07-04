package no.nav.metrics.aspects;

import mockit.*;
import no.nav.metrics.MetricsFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimerAspectTest {

    @Mocked
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mocked
    private Timed timed;

    @Mocked
    private MetricsFactory metricsFactory;

    @Mocked
    private Signature signature;

    @Before
    public void setup() throws Exception {
        new Expectations() {
            {
                timed.name();
                result = "";
            }
        };
    }

    @Test
    public void metricsProxySkalHaNavnLikNavnetBruktIAnnotasjonenDersomDetErSatt() throws Throwable {
        final String egendefinertNavn = "EtEgenDefinertNavn";
        new Expectations() {
            {
                timed.name();
                result = egendefinertNavn;
            }
        };

        new TimerAspect().timer(proceedingJoinPoint, timed);

        new Verifications() {
            {
                String navnBruktITimerProxy;
                MetricsFactory.createTimer(navnBruktITimerProxy = withCapture());
                times = 1;

                assertEquals(egendefinertNavn, navnBruktITimerProxy);
            }
        };
    }

    @Test
    public void metricsProxySkalHaNavnSomInneholderSimpleNameOgMetodeNavnDersomNavnIkkeErSattIAnnotasjon() throws Throwable {
        final String metodeNavn = "etMetodeNavn";

        new Expectations() {
            {
                proceedingJoinPoint.getSignature();
                result = signature;
                signature.getDeclaringType();
                result = Class.class;
                signature.getName();
                result = metodeNavn;
            }
        };

        new TimerAspect().timer(proceedingJoinPoint, timed);

        new Verifications() {
            {
                String navnBruktITimerProxy;
                MetricsFactory.createTimer(navnBruktITimerProxy = withCapture());
                times = 1;

                assertEquals(Class.class.getSimpleName() + "." + metodeNavn, navnBruktITimerProxy);
            }
        };
    }

    @Test(expected = EnException.class)
    public void exceptionsFraJoinPointProceedSkalKastesVidereFraAspektet() throws Throwable {
        new Expectations() {
            {
                proceedingJoinPoint.getSignature();
                result = signature;
                signature.getDeclaringType();
                result = Class.class;
                proceedingJoinPoint.proceed();
                result = new EnException();
            }
        };

        new TimerAspect().timer(proceedingJoinPoint, timed);
    }

    private class EnException extends Throwable {

    }
}