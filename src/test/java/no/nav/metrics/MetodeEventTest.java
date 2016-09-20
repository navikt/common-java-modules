package no.nav.metrics;

import mockit.Mocked;
import mockit.Verifications;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MetodeEventTest {

    @Test
    public void rapportererEventOmAltGarBra(@Mocked final Event event, @Mocked MetricsFactory factory) throws Throwable {
        Metodekall metodekall = new Metodekall() {
            @Override
            public Object kallMetode() throws Throwable {
                return "kall ok";
            }
        };

        Object resultat = MetodeEvent.eventForMetode(metodekall, "eventNavn");

        new Verifications() {{
            MetricsFactory.createEvent("eventNavn");
            event.report();
        }};

        assertEquals("kall ok", resultat);
    }

    @Test
    public void rapportererOmMetodenTryner(@Mocked final Event event) throws Throwable {
        Metodekall metodekall = new Metodekall() {
            @Override
            public Object kallMetode() throws Throwable {
                throw new RuntimeException("dummy");
            }
        };

        try {
            MetodeEvent.eventForMetode(metodekall, "eventNavn");
            fail("skal ha kastet exception");
        } catch (Exception e) {
            assertEquals("dummy", e.getMessage());
        }

        new Verifications() {{
            event.setFailed();
            event.report();
        }};
    }
}