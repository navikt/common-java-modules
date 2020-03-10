package no.nav.sbl.rest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.prometheus.MetricsTestUtils;
import no.nav.common.metrics.prometheus.MetricsTestUtils.PrometheusLine;
import no.nav.metrics.MetricsClient;
import no.nav.metrics.MetricsConfig;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.util.List;

import static ch.qos.logback.classic.Level.INFO;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.common.metrics.prometheus.MetricsTestUtils.equalCounter;
import static no.nav.sbl.util.LogUtils.setGlobalLogLevel;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class MetricsIntegrationTest {

    static {
        setGlobalLogLevel(INFO);
    }

    private static final int TIMEOUT = 3000;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

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
            RestUtils.withClient(c -> c.target("http://localhost:" + wireMockRule.port()).request().get());

            String sensuMetricMessage = IOUtils.toString(sensuServerSocketMock.accept().getInputStream(), Charset.defaultCharset());
            assertThat(sensuMetricMessage).isNotEmpty();
        }
    }

    @Test
    public void export_structured_metrics_for_rest_client() {
        givenThat(get(urlEqualTo("/")).willReturn(aResponse().withStatus(200)));
        givenThat(get(urlEqualTo("/timeout")).willReturn(aResponse().withStatus(200).withFixedDelay(2 * TIMEOUT)));

        getAndForget("http://127.0.0.1:" + wireMockRule.port());
        getAndForget("http://127.0.0.2:" + wireMockRule.port() + "/timeout");
        getAndForget("http://127.0.0.3:1234");
        getAndForget("http://does.not.exist");

        List<PrometheusLine> scrape = MetricsTestUtils.scrape();

        assertThat(scrape).anySatisfy(equalCounter(new PrometheusLine("rest_client_seconds_count", 1)
                .addLabel("host", "127.0.0.1")
                .addLabel("status", "200")
                .addLabel("error", "")
        ));

        assertThat(scrape).anySatisfy(equalCounter(new PrometheusLine("rest_client_seconds_count", 1)
                .addLabel("host", "127.0.0.2")
                .addLabel("status", "520")
                .addLabel("error", "SocketTimeoutException")
        ));

        assertThat(scrape).anySatisfy(equalCounter(new PrometheusLine("rest_client_seconds_count", 1)
                .addLabel("host", "127.0.0.3")
                .addLabel("status", "520")
                .addLabel("error", "SocketTimeoutException")
        ));

        assertThat(scrape).anySatisfy(equalCounter(new PrometheusLine("rest_client_seconds_count", 1)
                .addLabel("host", "does.not.exist")
                .addLabel("status", "520")
                .addLabel("error", "UnknownHostException")
        ));
    }

    private void getAndForget(String uri) {
        try {
            RestUtils.withClient(RestUtils.DEFAULT_CONFIG.withConnectTimeout(TIMEOUT).withReadTimeout(TIMEOUT), c -> c.target(uri).request().get());
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

}
