package no.nav.metrics.aspects;

import mockit.Expectations;
import mockit.Mocked;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Test;

import static org.junit.Assert.*;

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

    @Test
    public void lagKlasseTimernavn() {
        settOppNavneMock();

        String generertNavn = AspectUtil.lagKlasseTimernavn(joinPoint, "");
        String customNavn = AspectUtil.lagKlasseTimernavn(joinPoint, "customNavn");

        assertEquals("AspectUtilTest.metode1", generertNavn);
        assertEquals("customNavn.metode1", customNavn);
    }

    @Test
    public void metodeSkalIgnoreres() {
        String[] ignorerteMetoder = new String[]{"metode1", "metode2"};

        assertTrue(AspectUtil.metodeSkalIgnoreres("metode1", ignorerteMetoder));
        assertTrue(AspectUtil.metodeSkalIgnoreres("metode2", ignorerteMetoder));

        assertTrue(AspectUtil.metodeSkalIgnoreres("toString", ignorerteMetoder));
        assertTrue(AspectUtil.metodeSkalIgnoreres("hashCode", ignorerteMetoder));
        assertTrue(AspectUtil.metodeSkalIgnoreres("equals", ignorerteMetoder));

        assertFalse(AspectUtil.metodeSkalIgnoreres("metode3", ignorerteMetoder));
        assertFalse(AspectUtil.metodeSkalIgnoreres("metode4", ignorerteMetoder));
    }

    private void settOppNavneMock() {
        new Expectations() {{
            signature.getDeclaringType(); result = AspectUtilTest.class; // Bare for å ha noe
            signature.getName(); result = "metode1";
            joinPoint.getSignature(); result = signature;
        }};

    }
}