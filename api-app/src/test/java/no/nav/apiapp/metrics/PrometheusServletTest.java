package no.nav.apiapp.metrics;

import no.nav.apiapp.version.VersionService;
import no.nav.sbl.dialogarena.common.web.selftest.SelfTestService;
import no.nav.sbl.dialogarena.common.web.selftest.SelfTestStatus;
import no.nav.sbl.dialogarena.common.web.selftest.domain.Selftest;
import no.nav.sbl.dialogarena.common.web.selftest.domain.SelftestResult;
import no.nav.sbl.dialogarena.types.Pingable;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.apiapp.metrics.PrometheusServlet.Status.*;
import static no.nav.apiapp.metrics.PrometheusServlet.aggregertStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class PrometheusServletTest {

    private final SelfTestService selfTestService = mock(SelfTestService.class);
    private final VersionService versionService = mock(VersionService.class);
    private final PrometheusServlet prometheusServlet = new PrometheusServlet(selfTestService, versionService.getVersions());

    @Test
    public void smoketest() throws IOException {
        when(selfTestService.selfTest()).thenReturn(Selftest.builder()
                .checks(asList(
                        pingableOk("a"),
                        pingableKritiskFeil("b"),
                        pingableIkkeKritiskFeil("c")
                        ))
                .build());

        StringWriter writer = new StringWriter();

        PrometheusServlet.write(writer, prometheusServlet.selfTests());

        String output = writer.toString()
                .replaceAll("selftests_aggregate_result_time \\d+\\.0", "selftests_aggregate_result_time <TESTTIME>")
                .replaceAll("selftest_time\\{id=\"\\w\"\\,} \\d+\\.0", "selftest_time{id=\"<ID>\",} <TESTTIME>");

        String expectedOutput = IOUtils.toString(PrometheusServletTest.class.getResource("/PrometheusServletTest.txt"), "UTF-8");

        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    public void aggregertStatus_() {
        assertThat(aggregertStatus(emptyList())).isEqualTo(OK);
        assertThat(aggregertStatus(asList(okPing(), okPing()))).isEqualTo(OK);

        assertThat(aggregertStatus(asList(okPing(), kritiskFeil(), ikkeKritiskFeil(), okPing()))).isEqualTo(ERROR);
        assertThat(aggregertStatus(asList(okPing(), ikkeKritiskFeil(), kritiskFeil(), okPing()))).isEqualTo(ERROR);

        assertThat(aggregertStatus(asList(okPing(), ikkeKritiskFeil(), okPing()))).isEqualTo(WARNING);
    }

    private SelftestResult kritiskFeil() {
        return SelftestResult.builder()
                .critical(true)
                .result(SelfTestStatus.ERROR)
                .build();
    }

    private SelftestResult ikkeKritiskFeil() {
        return SelftestResult.builder()
                .result(SelfTestStatus.ERROR)
                .build();
    }

    private SelftestResult okPing() {
        return SelftestResult.builder()
                .result(SelfTestStatus.OK)
                .build();
    }

    private Pingable.Ping.PingMetadata metadata(boolean kritisk) {
        return metadata(kritisk, UUID.randomUUID().toString());
    }

    private Pingable.Ping.PingMetadata metadata(boolean kritisk, String id) {
        return new Pingable.Ping.PingMetadata(id, id, id, kritisk);
    }

    private SelftestResult pingableOk(String id) {
        return SelftestResult.builder()
                .id(id)
                .result(SelfTestStatus.OK)
                .build();
    }

    private SelftestResult pingableKritiskFeil(String id) {
        return SelftestResult.builder()
                .id(id)
                .result(SelfTestStatus.ERROR)
                .critical(true)
                .build();
    }

    private SelftestResult pingableIkkeKritiskFeil(String id) {
        return SelftestResult.builder()
                .id(id)
                .result(SelfTestStatus.ERROR)
                .build();
    }

}