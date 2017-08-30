package no.nav.dialogarena.config;

import no.nav.dialogarena.config.DevelopmentSecurity.ESSOSecurityConfig;
import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.dialogarena.config.DevelopmentSecurity.SamlSecurityConfig;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.modig.security.ws.UserSAMLOutInterceptor;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import org.junit.Test;

import java.io.File;

import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;


public class DevelopmentSecurityTest {

    @Test
    public void setupIntegrationTestSecurity() {
        DevelopmentSecurity.setupIntegrationTestSecurity(
                new DevelopmentSecurity.IntegrationTestConfig("veilarbsituasjon")
        );
    }

    @Test
    public void setupIntegrationTestSecurity_bakoverkompatibel() {
        DevelopmentSecurity.setupIntegrationTestSecurity(
               FasitUtils.getServiceUser("srvveilarbsituasjon", "veilarbsituasjon")
        );
    }

    @Test
    public void setupJettyWithISSO() {
        Jetty jetty = DevelopmentSecurity.setupISSO(
                jettyBuilder(),
                new ISSOSecurityConfig("veilarbsituasjon")
        ).buildJetty();
        jetty.start();
        jetty.stop.run();
    }
    @Test
    public void setupISSO() {
        DevelopmentSecurity.setupISSO(
                new ISSOSecurityConfig("veilarbsituasjon")
        );
    }

    @Test
    public void setupJettyWithESSO() {
        Jetty jetty = DevelopmentSecurity.setupESSO(
                jettyBuilder(),
                new ESSOSecurityConfig("veilarbsituasjonproxy")
        ).buildJetty();
        jetty.start();
        jetty.stop.run();
    }

    @Test
    public void setupESSO() {
        DevelopmentSecurity.setupESSO(
                new ESSOSecurityConfig("veilarbsituasjonproxy")
        );
    }

    @Test
    public void setupJettyWithSamlLogin() {
        Jetty jetty = DevelopmentSecurity.setupSamlLogin(
                jettyBuilder(),
                new SamlSecurityConfig("veilarbsituasjon")
        ).buildJetty();
        jetty.start();
        jetty.stop.run();
    }

    @Test
    public void setupIntegrationTestSecurity_kanByggeCxfClientMedSikkerhet() {
        setupIntegrationTestSecurity();

        new CXFClient<>(Aktoer_v2PortType.class)
                .withOutInterceptor(new UserSAMLOutInterceptor())
                .build();
    }

    private Jetty.JettyBuilder jettyBuilder() {
        return usingWar(new File("src/test/webapp"));
    }

}