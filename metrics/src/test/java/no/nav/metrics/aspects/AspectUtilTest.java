package no.nav.metrics.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AspectUtilTest {

    JoinPoint joinPoint = mock(JoinPoint.class);

    Signature signature = mock(Signature.class);

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
        when(signature.getDeclaringType()).thenReturn(AspectUtilTest.class); // Bare for å ha noe
        when(signature.getName()).thenReturn("metode1");
        when(joinPoint.getSignature()).thenReturn(signature);
    }
}
