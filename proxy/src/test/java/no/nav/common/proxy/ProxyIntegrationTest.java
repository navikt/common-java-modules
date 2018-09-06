package no.nav.common.proxy;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.log.LogFilter;
import no.nav.log.MDCConstants;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.util.LogUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.MDC;

import static ch.qos.logback.classic.Level.INFO;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.sbl.rest.RestUtils.withClient;
import static org.assertj.core.api.Assertions.assertThat;

public class ProxyIntegrationTest {

    static {
        LogUtils.setGlobalLogLevel(INFO);
    }

    private WireMockConfiguration wireMockConfig = WireMockConfiguration.wireMockConfig().port(8000);// No-args constructor defaults to port 8080

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig);

    private ProxyServlet proxyServlet;
    private Jetty jetty;

    @Before
    public void setup() {
        this.proxyServlet = new ProxyServlet(ProxyServletConfig.builder()
                .baseUrl("http://localhost:8000")
                .contextPath("/proxy")
                .id("proxy")
                .pingPath("/ping")
                .build()
        );
        this.jetty = startJetty(proxyServlet);
    }

    @After
    public void cleanup() {
        jetty.stop();
    }

    @Test
    public void request_proxied() {
        String correlationid = "correlationid";

        MDC.put(MDCConstants.MDC_CORRELATION_ID, correlationid);

        stubFor(get(urlMatching("/proxy/mock"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("mock!")
                )
        );

        withClient(c -> {
            String response = c.target("http://localhost:8080/proxy/mock").request().get(String.class);
            return assertThat(response).isEqualTo("mock!");
        });

        verify(getRequestedFor(urlEqualTo("/proxy/mock"))
                .withHeader(LogFilter.CORRELATION_ID_HEADER_NAME, equalTo(correlationid)));
    }

    @Test
    public void ping__error() {
        stubFor(get(urlMatching("/proxy/ping"))
                .willReturn(aResponse()
                        .withStatus(500)
                )
        );
        assertThat(proxyServlet.ping().erVellykket()).isFalse();
    }

    @Test
    public void ping__ok() {
        stubFor(get(urlMatching("/proxy/ping"))
                .willReturn(aResponse()
                        .withStatus(200)
                )
        );
        assertThat(proxyServlet.ping().erVellykket()).isTrue();
    }

    private static Jetty startJetty(ProxyServlet proxyServlet) {
        Jetty jetty = Jetty.usingWar()
                .at("/")
                .port(8080)
                .addHandler(proxyServlet.createHandler())
                .buildJetty();
        jetty.start();
        return jetty;
    }

}
