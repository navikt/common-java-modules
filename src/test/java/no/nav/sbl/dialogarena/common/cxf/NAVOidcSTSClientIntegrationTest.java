package no.nav.sbl.dialogarena.common.cxf;

import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.domain.SluttBruker;
import no.nav.dialogarena.mock.MockHandler;
import no.nav.modig.testcertificates.TestCertificates;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.security.auth.Subject;

import static no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY;
import static no.nav.modig.testcertificates.TestCertificates.setupKeyAndTrustStore;
import static no.nav.sbl.dialogarena.common.cxf.JettyTestServer.findFreePort;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.dialogarena.test.ssl.SSLTestUtils.disableCertificateChecks;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class NAVOidcSTSClientIntegrationTest {

    private static final Logger LOG = getLogger(NAVOidcSTSClientIntegrationTest.class);

    private Jetty stsMock;
    private MockHandler stsHandler = new MockHandler("sts");

    @Before
    public void setup() {
        disableCertificateChecks();
        setupKeyAndTrustStore();
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
    public void cache_sts_token_for_hver_bruker_og_stsType() throws Exception {
        Aktoer_v2PortType tjenesteA = new CXFClient<>(Aktoer_v2PortType.class)
                .address(url("tjeneste-a"))
                .configureStsForSystemUserInFSS()
                .build();
        Aktoer_v2PortType tjenesteB = new CXFClient<>(Aktoer_v2PortType.class)
                .address(url("tjeneste-b"))
                .configureStsForOnBehalfOfWithJWT()
                .build();
        Aktoer_v2PortType tjenesteC = new CXFClient<>(Aktoer_v2PortType.class)
                .address(url("tjeneste-c"))
                .configureStsForOnBehalfOfWithJWT()
                .build();

        setBrukerToken("jwt1");

        ping(tjenesteA);
        ping(tjenesteA);

        ping(tjenesteB);
        ping(tjenesteB);

        ping(tjenesteC);
        ping(tjenesteC);

        setBrukerToken("jwt2");

        ping(tjenesteA);
        ping(tjenesteA);

        ping(tjenesteB);
        ping(tjenesteB);

        ping(tjenesteC);
        ping(tjenesteC);

        assertThat(stsHandler.getRequestCount()).isEqualTo(
                2 + 2  // 2 sts-typer + 2 brukere
        );
    }

    private void setBrukerToken(String jwt) {
        Subject subject = new Subject();
        subject.getPublicCredentials().add(new OidcCredential(jwt));
        subject.getPrincipals().add(new SluttBruker(jwt, IdentType.InternBruker));
        new ThreadLocalSubjectHandler().setSubject(subject);
    }

    private void ping(Aktoer_v2PortType aktoer_v2PortType) {
        try {
            aktoer_v2PortType.ping();
        } catch (Throwable e) {
            LOG.warn("ping feilet: {}", e.getMessage());
        }
    }

    private String url(String tjenesteNavn) {
        return "https://localhost:1234/path/for/" + tjenesteNavn;
    }

}