package no.nav.sbl.dialogarena.common.cxf;

import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.dialogarena.config.ssl.SSLTestUtils;
import no.nav.dialogarena.mock.MockHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.security.auth.Subject;

import static no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY;
import static no.nav.dialogarena.config.ssl.SSLTestUtils.disableCertificateChecks;
import static no.nav.sbl.dialogarena.common.cxf.JettyTestServer.findFreePort;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class NAVOidcSTSClientIntegrationTest {

    private static final Logger LOG = getLogger(NAVOidcSTSClientIntegrationTest.class);

    private Jetty stsMock;
    private MockHandler stsHandler = new MockHandler("sts");

    @Before
    public void setup() {
        disableCertificateChecks();
        int port = findFreePort();
        Jetty jetty = Jetty.usingWar()
                .at("sts")
                .port(findFreePort())
                .sslPort(port)
                .overrideWebXml()
                .buildJetty();
        jetty.server.setHandler(stsHandler);
        jetty.start();
        stsMock = jetty;
        String stsUrl = String.format("https://localhost:%s/sts/NAVOidcSTSClientIntegrationTest", port);
        LOG.info("sts-url: {}", stsUrl);
        System.setProperty(STS_URL_KEY, stsUrl);
        System.setProperty(SYSTEMUSER_USERNAME, "username");
        System.setProperty(SYSTEMUSER_PASSWORD, "password");
        System.setProperty(SUBJECTHANDLER_KEY, ThreadLocalSubjectHandler.class.getName());
    }

    @After
    public void cleanup() {
        stsMock.stop.run();
    }

    @Test
    public void cache_sts_token_for_hvert_baksystem_for_systemSAML() throws Exception {
        cache_sts_token_for_hvert_baksystem();
    }

    @Test
    public void cache_sts_token_for_hvert_baksystem_for_issoToken() throws Exception {
        Subject subject = new Subject();
        subject.getPublicCredentials().add(new OidcCredential("jwt"));
        new ThreadLocalSubjectHandler().setSubject(subject);
        cache_sts_token_for_hvert_baksystem();
    }

    private void cache_sts_token_for_hvert_baksystem() {
        Aktoer_v2PortType tjenesteA = tjeneste("tjeneste-a");
        Aktoer_v2PortType tjenesteB = tjeneste("tjeneste-b");
        Aktoer_v2PortType tjenesteC = tjeneste("tjeneste-c");

        ping(tjenesteA);
        ping(tjenesteA);

        ping(tjenesteB);
        ping(tjenesteB);

        ping(tjenesteC);
        ping(tjenesteC);

        assertThat(stsHandler.getRequestCount()).isEqualTo(3);
    }


    private void ping(Aktoer_v2PortType aktoer_v2PortType) {
        try {
            aktoer_v2PortType.ping();
        } catch (Throwable e) {
            LOG.warn("ping feilet: {}", e.getMessage());
        }
    }

    private Aktoer_v2PortType tjeneste(String tjenesteNavn) {
        return new CXFClient<>(Aktoer_v2PortType.class)
                .address("https://localhost:1234/path/for/" + tjenesteNavn)
                .configureStsForSystemUserInFSS()
                .build();
    }

}