package no.nav.sbl.dialogarena.types;

import no.nav.sbl.dialogarena.types.Pingable.Ping;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.sbl.dialogarena.types.Get.pingResult;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PingableTest {

    @Mock
    Pingable sykTjeneste;

    @Mock
    Pingable friskTjeneste;

    @Mock
    Pingable avskruddTjeneste;

    @Before
    public void simulatePingResponses() {
        PingMetadata sykMetadata = new PingMetadata("sykTjeneste","sykTjeneste_v1", "en feilende tjeneste", false);
        PingMetadata friskMetadata = new PingMetadata("friskTjeneste","friskTjeneste_v1", "en frisk tjeneste", false);
        PingMetadata avskruddMetadata = new PingMetadata("avskruddTjeneste","\"avskrudd\"avskruddjeneste_v1", "en frisk tjeneste", false);

        when(sykTjeneste.ping()).thenReturn(Ping.feilet(sykMetadata, new IOException("tjenesten svarer ikke")));
        when(friskTjeneste.ping()).thenReturn(Ping.lyktes(friskMetadata));
        when(avskruddTjeneste.ping()).thenReturn(Ping.avskrudd(avskruddMetadata));
    }

    @Test
    public void predicateRecognisesDifferentPingResponses() {
        assertFalse(Get.vellykketPing().evaluate(sykTjeneste.ping()));
        assertTrue(Get.vellykketPing().evaluate(friskTjeneste.ping()));
        assertTrue(Get.avskruddPing().evaluate(avskruddTjeneste.ping()));
    }

    @Test
    public void findFailingPingRequests() {
        List<Ping> nonWorking = Stream.of(friskTjeneste, sykTjeneste)
                .map(pingResult()::transform)
                .filter(b -> !b.erVellykket())
                .collect(Collectors.toList());

        assertThat(nonWorking, hasSize(1));
        PingMetadata pingMetadata = nonWorking.get(0).getMetadata();
        assertThat(pingMetadata.getId(), is("sykTjeneste"));
        assertThat(pingMetadata.getEndepunkt(), is("sykTjeneste_v1"));
        boolean isCorrectException = nonWorking.get(0).getFeil() instanceof IOException;
        assertTrue(isCorrectException);
    }
}