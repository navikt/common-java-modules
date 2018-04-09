package no.nav.metrics;

import mockit.Mocked;
import mockit.Verifications;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MetodeTimerTest {

    @Test
    public void starterOgStopperTimerRundtFunksjon(@Mocked final Timer timer, @Mocked MetricsFactory factory) throws Throwable {
        Metodekall metodekall = new Metodekall() {
            @Override
            public Object kallMetode() throws Throwable {
                return "kall ok";
            }
        };

        Object resultat = MetodeTimer.timeMetode(metodekall, "timerNavn");

        new Verifications() {{
            MetricsFactory.createTimer("timerNavn");

            timer.start();
            timer.stop();
            timer.report();
        }};

        assertEquals("kall ok", resultat);
    }

    @Test
    public void rapportererFeilOmKalletTryner(@Mocked final Timer timer, @Mocked MetricsFactory factory) throws Throwable {
        Metodekall metodekall = new Metodekall() {
            @Override
            public Object kallMetode() throws Throwable {
                throw new RuntimeException("dummy");
            }
        };

        try {
            MetodeTimer.timeMetode(metodekall, "timerNavn");
            fail("Skal kaste exception");
        } catch (Throwable throwable) {
            assertEquals("dummy", throwable.getMessage());
        }

        new Verifications() {{
            timer.start();
            timer.setFailed();
            timer.stop();
            timer.report();
        }};

    }
}