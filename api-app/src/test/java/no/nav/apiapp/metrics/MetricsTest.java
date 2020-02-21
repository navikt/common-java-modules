package no.nav.apiapp.metrics;

import no.nav.apiapp.rest.PingResource;
import no.nav.common.metrics.prometheus.MetricsTestUtils;
import no.nav.common.metrics.prometheus.MetricsTestUtils.PrometheusLine;
import no.nav.fo.apiapp.JettyTest;
import no.nav.fo.apiapp.rest.RestEksempel;
import no.nav.sbl.dialogarena.test.junit.VirkerIkkeLokaltCategory;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.IntStream;

import static no.nav.apiapp.ApiAppServletContextListener.INTERNAL_METRICS;
import static no.nav.common.metrics.prometheus.MetricsTestUtils.equalCounter;
import static org.assertj.core.api.Assertions.assertThat;

public class MetricsTest extends JettyTest {

    @Test
    public void metrics() {
        sjekkOKStatus(INTERNAL_METRICS);
    }

    @Test
    @Category(VirkerIkkeLokaltCategory.class)
    public void exports_structured_api_metrics() {
        IntStream.range(0, 5).forEach(i -> get("/api/ping"));
        IntStream.range(0, 10).forEach(i -> get("/api/eksempel/konflikt"));

        String metrics = get(INTERNAL_METRICS).readEntity(String.class);
        List<PrometheusLine> prometheusLines = MetricsTestUtils.parse(metrics);

        assertThat(prometheusLines).anySatisfy(equalCounter(
                new PrometheusLine("rest_server_seconds_count", 5.0)
                        .addLabel("class", PingResource.class.getSimpleName())
                        .addLabel("method", "ping")
                        .addLabel("status", "200")
        ));

        assertThat(prometheusLines).anySatisfy(equalCounter(
                new PrometheusLine("rest_server_seconds_count", 10.0)
                        .addLabel("class", RestEksempel.class.getSimpleName())
                        .addLabel("method", "konflikt")
                        .addLabel("status", "400")
        ));
    }

    private void sjekkOKStatus(String path) {
        Response response = get(path);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(getString(path).toLowerCase()).doesNotContain(">error<");
    }

}
