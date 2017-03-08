package no.nav.sbl.dialogarena.common.cxf;

import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.net.ServerSocket;

import static java.lang.System.setProperty;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;

public class MetricsIntegrationTestServer extends JettyTestServer {

    @Test
    public void metrikkerSendesTilSensuSocket() throws Exception {
        try (ServerSocket sensuServerSocketMock = new ServerSocket(0)) {
            sensuServerSocketMock.setSoTimeout(5000);
            setProperty("sensu_client_port", Integer.toString(sensuServerSocketMock.getLocalPort()));

            String url = startCxfServer(Aktoer_v2PortType.class);

            new CXFClient<>(Aktoer_v2PortType.class)
                    .withMetrics() // TODO true by default ?
                    .address(url)
                    .build()
                    .ping();

            String sensuMetricMessage = IOUtils.toString(sensuServerSocketMock.accept().getInputStream());
            assertThat(sensuMetricMessage, not(isEmptyOrNullString()));
        }
    }


}