package no.nav.common.cxf;

import no.nav.common.auth.context.UserRole;
import no.nav.common.cxf.jetty.Jetty;
import no.nav.common.test.auth.AuthTestUtils;
import org.apache.servicemix.examples.cxf.HelloWorld;
import org.junit.*;
import org.slf4j.Logger;

import java.net.ConnectException;

import static no.nav.common.cxf.jetty.JettyTestServer.findFreePort;
import static no.nav.common.test.ssl.SSLTestUtils.disableCertificateChecks;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class NAVOidcSTSClientIntegrationTest {

    private static final Logger LOG = getLogger(NAVOidcSTSClientIntegrationTest.class);

    private Jetty stsMock;
    private MockHandler stsHandler = new MockHandler("sts");
    private String stsUrl;

    @Rule
    public AuthContextRule authContextRule = new AuthContextRule();

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
        stsUrl = String.format("https://localhost:%s/sts/NAVOidcSTSClientIntegrationTest", port);
        LOG.info("sts-url: {}", stsUrl);
    }

    @After
    public void cleanup() {
        stsMock.stop.run();
    }

    @Test
    @Ignore
    public void cache_sts_token_for_hver_bruker_og_stsType() throws Exception {
        StsConfig stsConfig = StsConfig.builder().url(stsUrl).username("username").password("password").build();

        HelloWorld tjenesteA = new CXFClient<>(HelloWorld.class)
                .address(url("tjeneste-a"))
                .configureStsForSystemUser(stsConfig)
                .build();
        HelloWorld tjenesteB = new CXFClient<>(HelloWorld.class)
                .address(url("tjeneste-b"))
                .configureStsForSubject(stsConfig)
                .build();
        HelloWorld tjenesteC = new CXFClient<>(HelloWorld.class)
                .address(url("tjeneste-c"))
                .configureStsForSubject(stsConfig)
                .build();
        HelloWorld tjenesteD = new CXFClient<>(HelloWorld.class)
                .address(url("tjeneste-d"))
                .configureStsForSubject(stsConfig)
                .build();

        setOidcToken("jwt1");

        ping(tjenesteA);
        ping(tjenesteA);

        ping(tjenesteB);
        ping(tjenesteB);

        ping(tjenesteC);
        ping(tjenesteC);

//        setEksternOpenAmToken("openam1");

        ping(tjenesteD);
        ping(tjenesteD);

        setOidcToken("jwt2");

        ping(tjenesteA);
        ping(tjenesteA);

        ping(tjenesteB);
        ping(tjenesteB);

        ping(tjenesteC);
        ping(tjenesteC);

//        setEksternOpenAmToken("openam2");

        ping(tjenesteD);
        ping(tjenesteD);

        assertThat(stsHandler.getRequestCount()).isEqualTo(
                        1 + // 1 systemSAML
                        2 + // oidc-tokens
                        2   // openam-tokens
        );
    }

    private void setOidcToken(String subject) {
        authContextRule.setAuthContext(AuthTestUtils.createAuthContext(UserRole.EKSTERN, subject));
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
