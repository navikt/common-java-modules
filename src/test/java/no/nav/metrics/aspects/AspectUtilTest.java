package no.nav.metrics.aspects;

import mockit.Expectations;
import mockit.Mocked;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AspectUtilTest {

    @Mocked
    JoinPoint joinPoint;

    @Mocked
    Signature signature;

    @Test
    public void lagMetodeTimernavn() {
        settOppNavneMock();

        String generertNavn = AspectUtil.lagMetodeTimernavn(joinPoint, "");
        String customNavn = AspectUtil.lagMetodeTimernavn(joinPoint, "customNavn");

        assertEquals("AspectUtilTest.metode1", generertNavn);
        assertEquals("customNavn", customNavn);
    }


    private void settOppNavneMock() {
        new Expectations() {{
            signature.getDeclaringType(); result = AspectUtilTest.class; // Bare for å ha noe
            signature.getName(); result = "metode1";
            joinPoint.getSignature(); result = signature;
        }};

    }
}