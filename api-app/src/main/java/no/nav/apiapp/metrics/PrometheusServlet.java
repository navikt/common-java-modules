package no.nav.apiapp.metrics;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;
import no.nav.sbl.dialogarena.common.web.selftest.SelfTestService;
import no.nav.sbl.dialogarena.common.web.selftest.domain.Selftest;
import no.nav.sbl.dialogarena.common.web.selftest.domain.SelftestResult;

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

import static io.prometheus.client.Collector.Type.GAUGE;
import static io.prometheus.client.Collector.Type.UNTYPED;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static no.nav.apiapp.util.StringUtils.of;

public class PrometheusServlet extends io.prometheus.client.exporter.MetricsServlet {

    public static final String SELFTESTS_AGGREGATE_RESULT = "selftests_aggregate_result";

    public static final String AGGREGATE_STATUS_ID = SELFTESTS_AGGREGATE_RESULT + "_status";
    public static final String AGGREGATE_TIME_ID = SELFTESTS_AGGREGATE_RESULT + "_time";

    public static final String SELFTEST_STATUS_ID = "selftest_status";
    public static final String SELFTEST_TIME_ID = "selftest_time";

    private final SelfTestService selfTestService;

    public PrometheusServlet(SelfTestService selfTestService) {
        this.selfTestService = selfTestService;
    }

    @Override
    public void init() throws ServletException {
        DefaultExports.initialize();
        super.init();
    }

    protected void doGet(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(TextFormat.CONTENT_TYPE_004);

        try(PrintWriter responseWriter = response.getWriter()){
            write(responseWriter, CollectorRegistry.defaultRegistry.metricFamilySamples());
            write(responseWriter, selfTests());
        }
    }

    static void write(Writer printWriter, Enumeration<Collector.MetricFamilySamples> samplesEnumeration) throws IOException {
        io.prometheus.client.exporter.common.TextFormat.write004(
                printWriter,
                samplesEnumeration
        );
    }

    Enumeration<Collector.MetricFamilySamples> selfTests() {
        long start = System.currentTimeMillis();
        Selftest selftest = selfTestService.selfTest();
        long tid = System.currentTimeMillis() - start;

        List<SelftestResult> selftestResults = selftest.getChecks();
        List<Collector.MetricFamilySamples> samples = new ArrayList<>();

        samples.add(new Collector.MetricFamilySamples(
                AGGREGATE_STATUS_ID, UNTYPED,
                "aggregert status for alle selftester. 0=ok, 1=kritisk feil, 2=ikke-kritisk feil",
                singletonList(aggregateSample(AGGREGATE_STATUS_ID, aggregertStatus(selftestResults).statusKode))
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
                selftestResults.stream().map(result -> pingSample(result, SELFTEST_STATUS_ID, status(result).statusKode)).collect(toList())
        ));

        samples.add(new Collector.MetricFamilySamples(
                SELFTEST_TIME_ID,
                GAUGE,
                "responstid for selftest",
                selftestResults.stream().map(result -> pingSample(result, SELFTEST_TIME_ID, result.getResponseTime())).collect(toList())
        ));

        return Collections.enumeration(samples);
    }

    static Status aggregertStatus(List<SelftestResult> selftestResults) {
        return selftestResults.stream()
                .map(PrometheusServlet::status)
                .max(Status::compareTo)
                .orElse(Status.OK);
    }

    private static Status status(SelftestResult selftestResult) {
        return selftestResult.harFeil() ? selftestResult.isCritical() ? Status.ERROR : Status.WARNING : Status.OK;
    }

    private Collector.MetricFamilySamples.Sample aggregateSample(String id, double value) {
        return new Collector.MetricFamilySamples.Sample(
                id,
                emptyList(),
                emptyList(),
                value
        );
    }

    private Collector.MetricFamilySamples.Sample pingSample(SelftestResult selftestResult, String sampleId, double value) {
        return new Collector.MetricFamilySamples.Sample(
                sampleId,
                singletonList("id"),
                singletonList(of(selftestResult.getId()).orElseThrow(IllegalArgumentException::new)),
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
