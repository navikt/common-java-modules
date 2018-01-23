package no.nav.apiapp.metrics;

import no.nav.sbl.dialogarena.types.Pingable;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.apiapp.metrics.PrometheusServlet.Status.*;
import static no.nav.apiapp.metrics.PrometheusServlet.aggregertStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class PrometheusServletTest {

    private final PrometheusServlet prometheusServlet = new PrometheusServlet();
    private Map<String, Pingable> pingables = new HashMap<>();

    @Before
    public void setup() {
        ApplicationContext applicationContext = prometheusServlet.applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(Pingable.class)).thenReturn(pingables);
    }

    @Test
    public void smoketest() throws IOException {
        pingables.put("a", pingableOk("a"));
        pingables.put("b", pingableKritiskFeil("b"));
        pingables.put("c", pingableIkkeKritiskFeil("c"));

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

    private Pingable.Ping kritiskFeil() {
        return Pingable.Ping.feilet(metadata(true), new RuntimeException());
    }

    private Pingable.Ping ikkeKritiskFeil() {
        return Pingable.Ping.feilet(metadata(false), new RuntimeException());
    }

    private Pingable.Ping okPing() {
        return Pingable.Ping.lyktes(metadata(true));
    }

    private Pingable.Ping.PingMetadata metadata(boolean kritisk) {
        return metadata(kritisk, UUID.randomUUID().toString());
    }

    private Pingable.Ping.PingMetadata metadata(boolean kritisk, String id) {
        return new Pingable.Ping.PingMetadata(id, id, id, kritisk);
    }

    private Pingable pingableOk(String id) {
        Pingable.Ping.PingMetadata metadata = metadata(true, id);
        return () -> Pingable.Ping.lyktes(metadata);
    }

    private Pingable pingableKritiskFeil(String id) {
        Pingable.Ping.PingMetadata metadata = metadata(true, id);
        return () -> Pingable.Ping.feilet(metadata, new RuntimeException());
    }

    private Pingable pingableIkkeKritiskFeil(String id) {
        Pingable.Ping.PingMetadata metadata = metadata(false, id);
        return () -> Pingable.Ping.feilet(metadata, new RuntimeException());
    }

}