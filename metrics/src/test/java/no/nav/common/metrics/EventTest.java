package no.nav.common.metrics;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EventTest {

    @Test
    public void kallTilMetricsClientSkalBenytteNavnPaEvent() {
        MetricsClient metricsClient = mock(MetricsClient.class);
        final String navnPaEvent = "navnPaEvent";

        new Event(metricsClient, navnPaEvent).report();

        verify(metricsClient).report(contains(navnPaEvent), any(),any(), anyLong());
    }
}
