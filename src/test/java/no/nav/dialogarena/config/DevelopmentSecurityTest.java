package no.nav.dialogarena.config;

import no.nav.dialogarena.config.DevelopmentSecurity.ESSOSecurityConfig;
import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.dialogarena.config.DevelopmentSecurity.SamlSecurityConfig;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.junit.Test;

import java.io.File;

import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;


public class DevelopmentSecurityTest {

    @Test
    public void setupIntegrationTestSecurity() {
        DevelopmentSecurity.setupIntegrationTestSecurity(
               FasitUtils.getServiceUser("srvveilarbsituasjon", "veilarbsituasjon", "t6")
        );
    }

    @Test
    public void setupJettyWithISSO() {
        Jetty jetty = DevelopmentSecurity.setupISSO(
                jettyBuilder(),
                new ISSOSecurityConfig("veilarbsituasjon", "t6")
        ).buildJetty();
        jetty.start();
        jetty.stop.run();
    }

    @Test
    public void setupJettyWithESSO() {
        Jetty jetty = DevelopmentSecurity.setupESSO(
                jettyBuilder(),
                new ESSOSecurityConfig("veilarbsituasjonproxy", "t6")
        ).buildJetty();
        jetty.start();
        jetty.stop.run();
    }

    @Test
    public void setupJettyWithSamlLogin() {
        Jetty jetty = DevelopmentSecurity.setupSamlLogin(
                jettyBuilder(),
                new SamlSecurityConfig("veilarbsituasjon", "t6")
        ).buildJetty();
        jetty.start();
        jetty.stop.run();
    }

    private Jetty.JettyBuilder jettyBuilder() {
        return usingWar(new File("src/test/webapp"));
    }

}