package no.nav.common.health.selftest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import okhttp3.OkHttpClient;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertTrue;

public class HealthCheckUtilsTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void pingUrl_skal_returnere_healthy() {
        String url = "http://localhost:" + wireMockRule.port();
        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        HealthCheckResult result = HealthCheckUtils.pingUrl(url, new OkHttpClient());

        assertTrue(result.isHealthy());
    }

    @Test
    public void pingUrl_skal_returnere_unhealthy_pa_uventet_status() {
        String url = "http://localhost:" + wireMockRule.port();
        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(500)));

        HealthCheckResult result = HealthCheckUtils.pingUrl(url, new OkHttpClient());

        assertTrue(result.isUnhealthy());
    }

    @Test
    public void pingUrl_skal_returnere_unhealthy_pa_host_unreachable() {
        HealthCheckResult result = HealthCheckUtils.pingUrl("http://localhost:1234", new OkHttpClient());
        assertTrue(result.isUnhealthy());
    }

}
