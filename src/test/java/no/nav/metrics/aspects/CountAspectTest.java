package no.nav.metrics.aspects;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import no.nav.metrics.MetricsFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Before;
import org.junit.Test;

import static no.nav.metrics.MetricsFactory.*;
import static org.junit.Assert.assertEquals;

public class CountAspectTest {

    @Mocked
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mocked
    private Count count;

    @Mocked
    private Signature signature;

    @Mocked
    private MetricsFactory metricsFactory;

    @Before
    public void setup() throws Exception {
        new Expectations() {
            {
                count.name();
                result = "";
            }
        };
    }

    @Test
    public void metricsProxySkalHaNavnLikNavnetBruktIAnnotasjonenDersomDetErSatt() throws Throwable {
        final String egendefinertNavn = "EtEgenDefinertNavn";
        new Expectations() {
            {
                count.name();
                result = egendefinertNavn;
            }
        };

        new CountAspect().count(proceedingJoinPoint, count);

        new Verifications() {
            {
                String navnBruktITimerProxy;
                createEvent(navnBruktITimerProxy = withCapture());
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

        new CountAspect().count(proceedingJoinPoint, count);

        new Verifications() {
            {
                String navnBruktITimerProxy;
                createEvent(navnBruktITimerProxy = withCapture());
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

        new CountAspect().count(proceedingJoinPoint, count);
    }

    private class EnException extends Throwable {

    }
}
