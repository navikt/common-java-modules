package no.nav.dialogarena.config;

import no.nav.dialogarena.config.DevelopmentSecurity.ESSOSecurityConfig;
import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.dialogarena.config.DevelopmentSecurity.SamlSecurityConfig;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.junit.Test;


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
                new Jetty.JettyBuilder().at("test"),
                new ISSOSecurityConfig("veilarbsituasjon", "t6")
        ).buildJetty();
        jetty.start();
        jetty.stop.run();
    }

    @Test
    public void setupJettyWithESSO() {
        Jetty jetty = DevelopmentSecurity.setupESSO(
                new Jetty.JettyBuilder().at("test"),
                new ESSOSecurityConfig("veilarbsituasjonproxy", "t6")
        ).buildJetty();
        jetty.start();
        jetty.stop.run();
    }

    @Test
    public void setupJettyWithSamlLogin() {
        Jetty jetty = DevelopmentSecurity.setupSamlLogin(
                new Jetty.JettyBuilder().at("test"),
                new SamlSecurityConfig("veilarbsituasjon", "t6")
        ).buildJetty();
        jetty.start();
        jetty.stop.run();
    }

}