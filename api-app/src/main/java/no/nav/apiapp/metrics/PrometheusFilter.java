package no.nav.apiapp.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Histogram;
import io.prometheus.client.filter.MetricsFilter;
import lombok.SneakyThrows;

import javax.servlet.FilterConfig;
import java.lang.reflect.Field;

/*
Extends the normal prometheus filter to correct an intentional design-flaw that prohibits multiple registrations of the filter.
See https://github.com/prometheus/client_java/issues/279
*/
public class PrometheusFilter extends MetricsFilter {

    private Histogram _histogram;

    public PrometheusFilter() {
        super("servlet_metrics", null, null, null);
    }

    @Override
    @SneakyThrows
    public void init(FilterConfig filterConfig) {
        super.init(filterConfig);
        Field histogramField = MetricsFilter.class.getDeclaredField("histogram");
        histogramField.setAccessible(true);
        this._histogram = (Histogram) histogramField.get(this);
    }

    @Override
    public void destroy() {
        super.destroy();
        CollectorRegistry.defaultRegistry.unregister(_histogram);
    }

}
