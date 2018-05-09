package no.nav.sbl.dialogarena.common.cxf;

import no.nav.metrics.MetricsClient;
import no.nav.metrics.MetricsConfig;
import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.net.ServerSocket;
import java.nio.charset.Charset;

import static no.nav.sbl.util.EnvironmentUtils.APP_NAME_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.FASIT_ENVIRONMENT_NAME_PROPERTY_NAME;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;

public class MetricsIntegrationTest extends JettyTestServer {

    @Test
    public void metrikkerSendesTilSensuSocket() throws Exception {
        try (ServerSocket sensuServerSocketMock = new ServerSocket(0)) {
            sensuServerSocketMock.setSoTimeout(5000);

            System.setProperty(APP_NAME_PROPERTY_NAME, "cxf");
            System.setProperty(FASIT_ENVIRONMENT_NAME_PROPERTY_NAME, "t");
            MetricsClient.resetMetrics(MetricsConfig.withSensuDefaults(MetricsConfig.builder()
                    .sensuHost("localhost")
                    .sensuPort(sensuServerSocketMock.getLocalPort())
                    .build()
            ));

            String url = startCxfServer(Aktoer_v2PortType.class);

            new CXFClient<>(Aktoer_v2PortType.class)
                    .withMetrics()
                    .address(url)
                    .build()
                    .ping();

            String sensuMetricMessage = IOUtils.toString(sensuServerSocketMock.accept().getInputStream(), Charset.defaultCharset());
            assertThat(sensuMetricMessage, not(isEmptyOrNullString()));
        }
    }


}