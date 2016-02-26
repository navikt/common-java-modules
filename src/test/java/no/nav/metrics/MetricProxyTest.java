package no.nav.metrics;

import org.junit.Test;

import java.io.IOException;

public class MetricProxyTest {

    @Test(expected = IOException.class)
    public void skalKasteUnderliggendeExceptionFraTjenesten() throws Exception {
        AnonymTjeneste anonymTjeneste = new AnonymTjeneste();
        Tjeneste proxy = MetricsFactory.createTimerProxy("navn", anonymTjeneste, Tjeneste.class);
        proxy.feilendeNettverksKall();
    }

    public interface Tjeneste{
        boolean feilendeNettverksKall() throws IOException;
    }

    public class AnonymTjeneste implements Tjeneste{
        public boolean feilendeNettverksKall() throws IOException {
            throw new IOException("feil");
        }
    }

}