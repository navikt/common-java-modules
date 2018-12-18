package no.nav.apiapp.metrics;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import no.nav.apiapp.rest.PingResource;
import no.nav.fo.apiapp.JettyTest;
import no.nav.fo.apiapp.rest.RestEksempel;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static no.nav.apiapp.ApiAppServletContextListener.INTERNAL_METRICS;
import static org.assertj.core.api.Assertions.assertThat;

public class MetricsTest extends JettyTest {

    @Test
    public void metrics() {
        sjekkOKStatus(INTERNAL_METRICS);
    }

    @Test
    public void exports_structured_api_metrics() {
        IntStream.range(0, 5).forEach(i -> get("/api/ping"));
        IntStream.range(0, 10).forEach(i -> get("/api/eksempel/konflikt"));

        String metrics = get(INTERNAL_METRICS).readEntity(String.class);
        List<PrometheusLine> prometheusLines = parse(metrics);

        assertThat(prometheusLines).anyMatch(matchCounter(
                new PrometheusLine("rest_server_seconds_count", 5.0)
                        .addLabel("class", PingResource.class.getSimpleName())
                        .addLabel("method", "ping")
                        .addLabel("status", "200")
        ));

        assertThat(prometheusLines).anyMatch(matchCounter(
                new PrometheusLine("rest_server_seconds_count", 10.0)
                        .addLabel("class", RestEksempel.class.getSimpleName())
                        .addLabel("method", "konflikt")
                        .addLabel("status", "400")
        ));
    }

    private Predicate<PrometheusLine> matchCounter(PrometheusLine expectedCounter) {
        return (l) -> l.metricName.equals(expectedCounter.metricName)
                && l.labels.equals(expectedCounter.labels)
                && l.value >= expectedCounter.value;
    }

    private List<PrometheusLine> parse(String prometheusScrape) {
        return stream(prometheusScrape.split("\n"))
                .filter(s -> !s.startsWith("#"))
                .map(this::parseLine)
                .collect(Collectors.toList());
    }

    private PrometheusLine parseLine(String prometheusScrape) {
        String regex = "^(\\w+)(\\{(.*)})? (.*?)$";

        assertThat(prometheusScrape).matches(regex);
        Matcher matcher = Pattern.compile(regex).matcher(prometheusScrape);
        assertThat(matcher.find()).isTrue();

        String metricName = matcher.group(1);
        String value = matcher.group(4);

        PrometheusLine prometheusLine = new PrometheusLine(metricName, Double.parseDouble(value));
        ofNullable(matcher.group(3)).ifPresent(labels -> stream(labels.split(",")).forEach(label -> {
            String[] split = label.split("=");
            String valueQuoted = split[1];
            prometheusLine.addLabel(split[0], valueQuoted.substring(1, valueQuoted.length() - 1));
        }));
        return prometheusLine;
    }

    private void sjekkOKStatus(String path) {
        Response response = get(path);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(getString(path).toLowerCase()).doesNotContain(">error<");
    }

    @EqualsAndHashCode
    @ToString
    private static class PrometheusLine {
        private final String metricName;
        private final double value;
        private final Map<String, String> labels = new HashMap<>();

        private PrometheusLine(String metricName, double value) {
            this.metricName = metricName;
            this.value = value;
        }

        private PrometheusLine addLabel(String name, String value) {
            labels.put(name, value);
            return this;
        }
    }

}
