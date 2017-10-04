package no.nav.apiapp.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;

public class PrometheusServlet extends io.prometheus.client.exporter.MetricsServlet {

    protected void doGet(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringWriter writer = new StringWriter();

        DefaultExports.initialize();
        io.prometheus.client.exporter.common.TextFormat.write004(
                writer,
                CollectorRegistry.defaultRegistry.metricFamilySamples()
        );
        response.getWriter().write(writer.toString());
    }
}
