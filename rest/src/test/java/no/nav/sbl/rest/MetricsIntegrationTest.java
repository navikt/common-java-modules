package no.nav.sbl.rest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.metrics.MetricsClient;
import no.nav.metrics.MetricsConfig;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.util.EnvironmentUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

import java.net.ServerSocket;
import java.nio.charset.Charset;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.sbl.util.EnvironmentUtils.APP_NAME_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.FASIT_ENVIRONMENT_NAME_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class MetricsIntegrationTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(58089);

    @Test
    public void metrikkerSendesTilSensuSocket() throws Exception {
        try (ServerSocket sensuServerSocketMock = new ServerSocket(0)) {
            sensuServerSocketMock.setSoTimeout(5000);

            MetricsClient.resetMetrics(MetricsConfig.withSensuDefaults(MetricsConfig.builder()
                    .sensuHost("localhost")
                    .sensuPort(sensuServerSocketMock.getLocalPort())
                    .build()
            ));

            givenThat(get(urlEqualTo("/")).willReturn(aResponse().withStatus(200)));
            RestUtils.withClient(c -> c.target("http://localhost:58089").request().get());

            String sensuMetricMessage = IOUtils.toString(sensuServerSocketMock.accept().getInputStream(), Charset.defaultCharset());
            assertThat(sensuMetricMessage).isNotEmpty();
        }
    }
}