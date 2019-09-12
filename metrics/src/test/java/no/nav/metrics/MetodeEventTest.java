package no.nav.metrics;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MetodeEventTest {

    @Test
    public void rapportererEventOmAltGarBra() throws Throwable {
        Event event = mock(Event.class);

        Metodekall metodekall = () -> "kall ok";

        Object resultat = MetodeEvent.eventForMetode(metodekall, "eventNavn", null, str -> event);

        verify(event).report();

        assertEquals("kall ok", resultat);
    }

    @Test
    public void rapportererOmMetodenTryner() throws Throwable {
        Event event = mock(Event.class);

        Metodekall metodekall = () -> {
            throw new RuntimeException("dummy");
        };

        try {
            MetodeEvent.eventForMetode(metodekall, "eventNavn", null, str -> event);
            fail("skal ha kastet exception");
        } catch (Exception e) {
            assertEquals("dummy", e.getMessage());
        }

        verify(event).setFailed();
        verify(event).report();
    }

    @Test
    public void markererCheckedExceptions() throws Throwable {
        Event event = mock(Event.class);

        Metodekall metodekall = () -> {
            throw new IOException("dummy");
        };

        try {
            MetodeEvent.eventForMetode(metodekall, "eventNavn", null, str -> event);
            fail("skal ha kastet exception");
        } catch (Exception e) {
            assertEquals("dummy", e.getMessage());
        }

        verify(event).setFailed();
        verify(event).addFieldToReport("checkedException", true);
        verify(event).report();
    }
}
