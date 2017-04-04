package no.nav.dialogarena.config;

import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.junit.Test;


public class DevelopmentSecurityTest {

    @Test
    public void setupJettyWithISSO() {
        Jetty jetty = DevelopmentSecurity.setupISSO(
                new Jetty.JettyBuilder().at("test"),
                new ISSOSecurityConfig("veilarbsituasjon", "t6")
        ).buildJetty();
        jetty.start();
        jetty.stop.run();
    }

}