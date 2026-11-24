package no.nav.common.health.selftest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static no.nav.common.test.wiremock.WireMockTestUtils.createWireMockExtension;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class HealthCheckUtilsTest {

    @RegisterExtension
    public WireMockExtension wireMockRule = createWireMockExtension();

    @Test
    public void pingUrl_skal_returnere_healthy() {
        String url = "http://localhost:" + wireMockRule.getPort();
        wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        HealthCheckResult result = HealthCheckUtils.pingUrl(url, new OkHttpClient());

        assertTrue(result.isHealthy());
    }

    @Test
    public void pingUrl_skal_returnere_unhealthy_pa_uventet_status() {
        String url = "http://localhost:" + wireMockRule.getPort();
        wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

        HealthCheckResult result = HealthCheckUtils.pingUrl(url, new OkHttpClient());

        assertTrue(result.isUnhealthy());
    }

    @Test
    public void pingUrl_skal_returnere_unhealthy_pa_host_unreachable() {
        HealthCheckResult result = HealthCheckUtils.pingUrl("http://localhost:1234", new OkHttpClient());
        assertTrue(result.isUnhealthy());
    }
}
