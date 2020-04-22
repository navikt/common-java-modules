package no.nav.common.metrics.prometheus;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import no.nav.common.metrics.MetricsFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

public class MetricsTestUtils {

    public static List<PrometheusLine> scrape(){
        PrometheusMeterRegistry prometheusMeterRegistry = (PrometheusMeterRegistry) MetricsFactory.getMeterRegistry();
        return MetricsTestUtils.parse(prometheusMeterRegistry.scrape());
    }

    public static List<PrometheusLine> parse(String prometheusScrape) {
        return stream(prometheusScrape.split("\n"))
                .filter(s -> !s.startsWith("#"))
                .map(MetricsTestUtils::parseLine)
                .collect(Collectors.toList());
    }

    private static PrometheusLine parseLine(String prometheusScrape) {
        String regex = "^(\\w+)(\\{(.*)})? (.*?)$";

        assertThat(prometheusScrape).matches(regex);
        Matcher matcher = Pattern.compile(regex).matcher(prometheusScrape);
        assertThat(matcher.find()).isTrue();

        String metricName = matcher.group(1);
        String value = matcher.group(4);

        PrometheusLine prometheusLine = new PrometheusLine(metricName, Double.parseDouble(value));
        ofNullable(matcher.group(3)).ifPresent(labels -> stream(labels.split(",")).forEach(label -> {
            if (!label.contains("=")) {
                return;
            }

            String[] split = label.split("=");
            String valueQuoted = split[1];

            prometheusLine.addLabel(split[0], valueQuoted.substring(1, valueQuoted.length() - 1));
        }));

        return prometheusLine;
    }

    public static Consumer<PrometheusLine> equalCounter(PrometheusLine expectedCounter) {
        return (prometheusLine) -> {
            assertThat(prometheusLine.metricName).isEqualTo(expectedCounter.metricName);
            assertThat(prometheusLine.labels).isEqualTo(expectedCounter.labels);
            assertThat(prometheusLine.value).isGreaterThanOrEqualTo(expectedCounter.value);
        };
    }

    @EqualsAndHashCode
    @ToString
    public static class PrometheusLine {
        private final String metricName;
        private final double value;
        private final Map<String, String> labels = new HashMap<>();

        public PrometheusLine(String metricName, double value) {
            this.metricName = metricName;
            this.value = value;
        }

        public PrometheusLine addLabel(String name, String value) {
            labels.put(name, value);
            return this;
        }
    }

}
