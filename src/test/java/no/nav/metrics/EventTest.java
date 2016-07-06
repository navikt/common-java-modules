package no.nav.metrics;

import mockit.Mocked;
import mockit.Verifications;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;

public class EventTest {

    @Mocked
    private MetricsClient metricsClient;

    @Test
    @SuppressWarnings("unchecked")
    public void kallTilMetricsClientSkalBenytteNavnPaEvent() throws Exception {
        final String navnPaEvent = "navnPaEvent";

        new Event(metricsClient, navnPaEvent).report();

        new Verifications() {
            {
                String metricName;
                metricsClient.report(metricName = withCapture(), (Map<String, Object>) any, anyLong);
                times = 1;

                assertTrue(metricName.contains(navnPaEvent));
            }
        };
    }
}
