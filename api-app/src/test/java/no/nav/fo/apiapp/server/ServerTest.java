package no.nav.fo.apiapp.server;

import no.nav.fo.apiapp.StartJetty;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.junit.Test;

import static no.nav.fo.apiapp.JettyTest.tilfeldigPort;

public class ServerTest {

    @Test
    public void startOgStopp() {
        Jetty jetty = StartJetty.nyJetty(ServerTest.class.getSimpleName(), tilfeldigPort());
        jetty.start();
        jetty.stop.run();
    }

}
