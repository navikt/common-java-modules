package no.nav.common.cxf;

import no.nav.common.auth.SubjectRule;
import no.nav.common.auth.TestSubjectUtils;
import no.nav.common.cxf.jetty.Jetty;
import org.apache.servicemix.examples.cxf.HelloWorld;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

import java.net.ConnectException;

import static no.nav.common.auth.SsoToken.Type.EKSTERN_OPENAM;
import static no.nav.common.auth.SsoToken.Type.OIDC;
import static no.nav.common.cxf.StsSecurityConstants.*;
import static no.nav.common.cxf.jetty.JettyTestServer.findFreePort;
import static no.nav.common.test.ssl.SSLTestUtils.disableCertificateChecks;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class NAVOidcSTSClientIntegrationTest {

    private static final Logger LOG = getLogger(NAVOidcSTSClientIntegrationTest.class);

    private Jetty stsMock;
    private MockHandler stsHandler = new MockHandler("sts");

    @Rule
    public SubjectRule subjectRule = new SubjectRule();

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
    }

    @After
    public void cleanup() {
        stsMock.stop.run();
    }

    @Test
    public void cache_sts_token_for_hver_bruker_og_stsType() throws Exception {
        HelloWorld tjenesteA = new CXFClient<>(HelloWorld.class)
                .address(url("tjeneste-a"))
                .configureStsForSystemUserInFSS()
                .build();
        HelloWorld tjenesteB = new CXFClient<>(HelloWorld.class)
                .address(url("tjeneste-b"))
                .configureStsForOnBehalfOfWithJWT()
                .build();
        HelloWorld tjenesteC = new CXFClient<>(HelloWorld.class)
                .address(url("tjeneste-c"))
                .configureStsForOnBehalfOfWithJWT()
                .build();
        HelloWorld tjenesteD = new CXFClient<>(HelloWorld.class)
                .address(url("tjeneste-d"))
                .configureStsForExternalSSO()
                .build();

        setOidcToken("jwt1");

        ping(tjenesteA);
        ping(tjenesteA);

        ping(tjenesteB);
        ping(tjenesteB);

        ping(tjenesteC);
        ping(tjenesteC);

        setEksternOpenAmToken("openam1");

        ping(tjenesteD);
        ping(tjenesteD);

        setOidcToken("jwt2");

        ping(tjenesteA);
        ping(tjenesteA);

        ping(tjenesteB);
        ping(tjenesteB);

        ping(tjenesteC);
        ping(tjenesteC);

        setEksternOpenAmToken("openam2");

        ping(tjenesteD);
        ping(tjenesteD);

        assertThat(stsHandler.getRequestCount()).isEqualTo(
                        1 + // 1 systemSAML
                        2 + // oidc-tokens
                        2   // openam-tokens
        );
    }

    private void setOidcToken(String jwt) {
        subjectRule.setSubject(TestSubjectUtils.builder()
                .tokenType(OIDC)
                .token(jwt)
                .build()
        );
    }

    private void setEksternOpenAmToken(String openAmToken) {
        subjectRule.setSubject(TestSubjectUtils.builder()
                .tokenType(EKSTERN_OPENAM)
                .token(openAmToken)
                .build()
        );
    }

    private void ping(HelloWorld aktoer_v2PortType) {
        try {
            aktoer_v2PortType.sayHi("hi");
        } catch (Throwable e) {
            assertThat(e).hasRootCauseInstanceOf(ConnectException.class);
            LOG.warn("ping feilet: {}", e.getMessage());
        }
    }

    private String url(String tjenesteNavn) {
        return "https://localhost:1234/path/for/" + tjenesteNavn;
    }

}