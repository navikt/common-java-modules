package no.nav.sbl.dialogarena.types;
import no.nav.sbl.dialogarena.types.Pingable.Ping;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static no.nav.modig.lang.collections.IterUtils.on;
import static no.nav.modig.lang.collections.PredicateUtils.not;
import static no.nav.sbl.dialogarena.types.Get.pingResult;
import static no.nav.sbl.dialogarena.types.Get.vellykketPing;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PingableTest {

    @Mock
    Pingable sykTjeneste;

    @Mock
    Pingable friskTjeneste;

    @Before
    public void simulatePingResponses() {
        when(sykTjeneste.ping()).thenReturn(Ping.feilet("sykTjeneste_v1", "en SOAP-tjeneste som ikke funker", true, new IOException("tjenesten svarer ikke")));
        when(friskTjeneste.ping()).thenReturn(Ping.lyktes("friskTjeneste_v1", "En tjeneste som funker", true));
    }

    @Test
    public void predicateRecognisesDifferentPingResponses() {
        assertFalse(Get.vellykketPing().evaluate(sykTjeneste.ping()));
        assertTrue(Get.vellykketPing().evaluate(friskTjeneste.ping()));
    }

    @Test
    public void findFailingPingRequests() {
        List<Ping> nonWorking = on(asList(friskTjeneste, sykTjeneste)).map(pingResult()).filter(not(vellykketPing())).collect();

        assertThat(nonWorking, hasSize(1));
        assertThat(nonWorking.get(0).getEndepunkt(), is("sykTjeneste_v1"));
        boolean isCorrectException = nonWorking.get(0).getFeil() instanceof IOException;
        assertTrue(isCorrectException);
    }
}