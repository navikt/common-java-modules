package no.nav.apiapp.metrics;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.DefaultExports;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import static io.prometheus.client.Collector.Type.GAUGE;
import static io.prometheus.client.Collector.Type.UNTYPED;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.apiapp.ServletUtil.getContext;

public class PrometheusServlet extends io.prometheus.client.exporter.MetricsServlet {

    public static final String SELFTESTS_AGGREGATE_RESULT = "selftests_aggregate_result";

    public static final String AGGREGATE_STATUS_ID = SELFTESTS_AGGREGATE_RESULT + "_status";
    public static final String AGGREGATE_TIME_ID = SELFTESTS_AGGREGATE_RESULT + "_time";

    public static final String SELFTEST_STATUS_ID = "selftest_status";
    public static final String SELFTEST_TIME_ID = "selftest_time";

    ApplicationContext applicationContext;

    @Override
    public void init() throws ServletException {
        DefaultExports.initialize();
        applicationContext = getContext(getServletContext());
        super.init();
    }

    protected void doGet(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter responseWriter = response.getWriter();
        write(responseWriter, CollectorRegistry.defaultRegistry.metricFamilySamples());
        write(responseWriter, selfTests());
    }

    static void write(Writer printWriter, Enumeration<Collector.MetricFamilySamples> samplesEnumeration) throws IOException {
        io.prometheus.client.exporter.common.TextFormat.write004(
                printWriter,
                samplesEnumeration
        );
    }

    Enumeration<Collector.MetricFamilySamples> selfTests() {
        long start = System.currentTimeMillis();
        List<Ping> pings = applicationContext.getBeansOfType(Pingable.class)
                .values()
                .stream()
                .map(this::ping)
                .collect(Collectors.toList());
        long tid = System.currentTimeMillis() - start;

        List<Collector.MetricFamilySamples> samples = new ArrayList<>();

        samples.add(new Collector.MetricFamilySamples(
                AGGREGATE_STATUS_ID, UNTYPED,
                "aggregert status for alle selftester. 0=ok, 1=kritisk feil, 2=ikke-kritisk feil",
                singletonList(aggregateSample(AGGREGATE_STATUS_ID, aggregertStatus(pings).statusKode))
        ));

        samples.add(new Collector.MetricFamilySamples(
                AGGREGATE_TIME_ID, GAUGE,
                "total tid alle selftester",
                singletonList(aggregateSample(AGGREGATE_TIME_ID, tid))
        ));

        samples.add(new Collector.MetricFamilySamples(
                SELFTEST_STATUS_ID,
                UNTYPED,
                "status for selftest. 0=ok, 1=kritisk feil, 2=ikke-kritisk feil",
                pings.stream().map(ping -> pingSample(ping, SELFTEST_STATUS_ID, status(ping).statusKode)).collect(Collectors.toList())
        ));

        samples.add(new Collector.MetricFamilySamples(
                SELFTEST_TIME_ID,
                GAUGE,
                "responstid for selftest",
                pings.stream().map(ping -> pingSample(ping, SELFTEST_TIME_ID, ping.getResponstid())).collect(Collectors.toList())
        ));

        return Collections.enumeration(samples);
    }

    private Ping ping(Pingable pingable) {
        long start = System.currentTimeMillis();
        Ping ping = pingable.ping();
        return ping.setResponstid(System.currentTimeMillis() - start);
    }

    static Status aggregertStatus(List<Ping> pings) {
        return pings.stream()
                .map(PrometheusServlet::status)
                .max(Status::compareTo)
                .orElse(Status.OK);
    }

    private static Status status(Ping ping) {
        return ping.erVellykket() ? Status.OK : ping.getMetadata().isKritisk() ? Status.ERROR : Status.WARNING;
    }

    private Collector.MetricFamilySamples.Sample aggregateSample(String id, double value) {
        return new Collector.MetricFamilySamples.Sample(
                id,
                emptyList(),
                emptyList(),
                value
        );
    }

    private Collector.MetricFamilySamples.Sample pingSample(Ping ping, String sampleId, double value) {
        return new Collector.MetricFamilySamples.Sample(
                sampleId,
                singletonList("id"),
                singletonList(ping.getMetadata().getId()),
                value
        );
    }

    enum Status {
        OK(0),
        WARNING(2),
        ERROR(1);

        private final int statusKode;

        Status(int statusKode) {
            this.statusKode = statusKode;
        }
    }

}
