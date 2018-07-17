package no.nav.apiapp.metrics;

import org.junit.Test;

import javax.servlet.FilterConfig;

import static org.mockito.Mockito.mock;

public class PrometheusFilterTest {

    @Test
    public void can_reinitialize() {
        PrometheusFilter prometheusFilter = new PrometheusFilter();
        FilterConfig filterConfig = mock(FilterConfig.class);

        prometheusFilter.init(filterConfig);
        prometheusFilter.destroy();

        prometheusFilter.init(filterConfig);
        prometheusFilter.destroy();

        prometheusFilter.init(filterConfig);
        prometheusFilter.destroy();
    }

}