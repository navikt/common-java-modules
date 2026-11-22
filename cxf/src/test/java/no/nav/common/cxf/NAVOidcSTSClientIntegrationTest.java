package no.nav.common.cxf;

import static no.nav.common.cxf.jetty.JettyTestServer.findFreePort;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.net.ConnectException;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.UserRole;
import no.nav.common.cxf.jetty.Jetty;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.test.ssl.CxfCertificateValidationDisabler;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.servicemix.examples.cxf.HelloWorld;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Slf4j
public class NAVOidcSTSClientIntegrationTest {
    private Jetty stsMock;
    private final MockHandler stsHandler = new MockHandler("sts");
    private String stsUrl;
    private Bus testBus;

    @Rule
    public AuthContextRule authContextRule = new AuthContextRule();

    @Before
    @SuppressWarnings("deprecation") // STS-integrasjonen bruker den beholdte CXF-bussadapteren.
    public void setup() throws IOException {
        testBus = BusFactory.newInstance().createBus();
        new CxfCertificateValidationDisabler().initialize(testBus);
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
        log.info("sts-url: {}", stsUrl);
    }

    @After
    public void cleanup() {
        stsMock.stop.run();
        testBus.shutdown(true);
    }

    @Test
    @Ignore
    public void cache_sts_token_for_hver_bruker_og_stsType() {
        // Arrange
        var stsConfig = StsConfig.builder()
                .url(stsUrl)
                .username("username")
                .password("password")
                .build();

        HelloWorld tjenesteA = testClient()
                .address(url("tjeneste-a"))
                .configureStsForSystemUser(stsConfig)
                .build();
        HelloWorld tjenesteB = testClient()
                .address(url("tjeneste-b"))
                .configureStsForSubject(stsConfig)
                .build();
        HelloWorld tjenesteC = testClient()
                .address(url("tjeneste-c"))
                .configureStsForSubject(stsConfig)
                .build();
        HelloWorld tjenesteD = testClient()
                .address(url("tjeneste-d"))
                .configureStsForSubject(stsConfig)
                .build();

        setOidcToken("jwt1");

        // Act
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

        // Assert
        assertThat(stsHandler.getRequestCount())
                .isEqualTo(
                        1
                                + // 1 systemSAML
                                2
                                + // oidc-tokens
                                2 // openam-tokens
                        );
    }

    private void setOidcToken(String subject) {
        authContextRule.setAuthContext(AuthTestUtils.createAuthContext(UserRole.EKSTERN, subject));
    }

    private CXFClient<HelloWorld> testClient() {
        var client = new CXFClient<>(HelloWorld.class);
        client.factoryBean.setBus(testBus);
        return client;
    }

    private void ping(HelloWorld aktoer_v2PortType) {
        try {
            aktoer_v2PortType.sayHi("hi");
        } catch (Throwable e) {
            assertThat(e).hasRootCauseInstanceOf(ConnectException.class);
            log.warn("ping feilet: {}", e.getMessage());
        }
    }

    private String url(String tjenesteNavn) {
        return "https://localhost:1234/path/for/" + tjenesteNavn;
    }
}
