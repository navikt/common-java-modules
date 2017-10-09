package no.nav.apiapp.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.DefaultExports;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PrometheusServlet extends io.prometheus.client.exporter.MetricsServlet {

    protected void doGet(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        DefaultExports.initialize();
        io.prometheus.client.exporter.common.TextFormat.write004(
                response.getWriter(),
                CollectorRegistry.defaultRegistry.metricFamilySamples()
        );
    }
}
