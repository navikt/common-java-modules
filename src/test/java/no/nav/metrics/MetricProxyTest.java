package no.nav.metrics;

import mockit.Mocked;
import no.nav.metrics.proxy.EventProxy;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

public class MetricProxyTest {

    @Test(expected = IOException.class)
    public void skalKasteUnderliggendeExceptionFraTjenesten(@Mocked Timer timer) throws Exception {
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

    @Test(expected = IllegalStateException.class)
    public void proxySkalKasteIllegalStateExceptionDersomIncludeMethodsErGjortOgExcludeMethodsKalles() throws Exception {
        final EventProxy eventProxy = new EventProxy("navPaEvent", new Object());
        eventProxy.includeMethods(Collections.singletonList("enMetodeSomSkalInkluderes"));
        eventProxy.excludeMethods(Collections.singletonList("enMetodeSomSkalEkskluderes"));
    }

    @Test(expected = IllegalStateException.class)
    public void proxySkalKasteIllegalStateExceptionDersomExcludeMethodsErGjortOgIncludeMethodsKalles() throws Exception {
        final EventProxy eventProxy = new EventProxy("navPaEvent", new Object());
        eventProxy.excludeMethods(Collections.singletonList("enMetodeSomSkalEkskluderes"));
        eventProxy.includeMethods(Collections.singletonList("enMetodeSomSkalInkluderes"));
    }
}