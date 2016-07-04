package no.nav.metrics;

import org.junit.Test;

import java.io.IOException;

public class MetricProxyTest {

    @Test(expected = IOException.class)
    public void skalKasteUnderliggendeExceptionFraTjenesten() throws Exception {
        Tjeneste proxy = MetricsFactory.createTimerProxy("navn", new FeilendeTjeneste(), Tjeneste.class);
        proxy.feilendeNettverkskall();
    }

    public interface Tjeneste {
        boolean feilendeNettverkskall() throws IOException;
    }

    public class FeilendeTjeneste implements Tjeneste {
        public boolean feilendeNettverkskall() throws IOException {
            throw new IOException("feil");
        }
    }
}