package no.nav.sbl.rest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

import java.net.ServerSocket;
import java.nio.charset.Charset;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.lang.System.setProperty;
import static no.nav.metrics.handlers.SensuHandler.SENSU_CLIENT_PORT;
import static org.assertj.core.api.Assertions.assertThat;

public class MetricsIntegrationTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(58089);

    @Test
    public void metrikkerSendesTilSensuSocket() throws Exception {
        try (ServerSocket sensuServerSocketMock = new ServerSocket(0)) {
            sensuServerSocketMock.setSoTimeout(5000);
            setProperty(SENSU_CLIENT_PORT, Integer.toString(sensuServerSocketMock.getLocalPort()));

            givenThat(get(urlEqualTo("/")).willReturn(aResponse().withStatus(200)));
            RestUtils.withClient(c -> c.target("http://localhost:58089").request().get());

            String sensuMetricMessage = IOUtils.toString(sensuServerSocketMock.accept().getInputStream(), Charset.defaultCharset());
            assertThat(sensuMetricMessage).isNotEmpty();
        }
    }
}