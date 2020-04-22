package no.nav.common.metrics;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class MetodeTimerTest {
    private Timer timer = mock(Timer.class);

    @Before
    public void setup() {
        when(timer.stop()).thenReturn(timer);
    }

    @Test
    public void starterOgStopperTimerRundtFunksjon() throws Throwable {
        Metodekall metodekall = () -> "kall ok";


        Object resultat = MetodeTimer.timeMetode(metodekall, "timerNavn", str -> timer);

        verify(timer).start();
        verify(timer).stop();
        verify(timer).report();

        assertEquals("kall ok", resultat);
    }

    @Test
    public void rapportererFeilOmKalletTryner() throws Throwable {
        Metodekall metodekall = () -> {
            throw new RuntimeException("dummy");
        };
        when(timer.stop()).thenReturn(timer);

        try {
            MetodeTimer.timeMetode(metodekall, "timerNavn", str -> timer);
            fail("Skal kaste exception");
        } catch (Throwable throwable) {
            assertEquals("dummy", throwable.getMessage());
        }

        verify(timer).start();
        verify(timer).setFailed();
        verify(timer).addFieldToReport("checkedException", false);
        verify(timer).stop();
        verify(timer).report();
    }

    @Test
    public void markererCheckedExceptions() throws Throwable {
        Metodekall metodekall = () -> {
            throw new IOException("dummy");
        };

        try {
            MetodeTimer.timeMetode(metodekall, "timerNavn", str -> timer);
            fail("Skal kaste exception");
        } catch (Throwable throwable) {
            assertEquals("dummy", throwable.getMessage());
        }

        verify(timer).start();
        verify(timer).setFailed();
        verify(timer).addFieldToReport("checkedException", true);
        verify(timer).stop();
        verify(timer).report();
    }
}
