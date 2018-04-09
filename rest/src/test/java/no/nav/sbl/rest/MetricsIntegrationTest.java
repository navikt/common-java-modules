package no.nav.sbl.rest;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.net.ServerSocket;
import java.nio.charset.Charset;

import static java.lang.System.setProperty;
import static no.nav.metrics.handlers.SensuHandler.SENSU_CLIENT_PORT;
import static org.assertj.core.api.Assertions.assertThat;

public class MetricsIntegrationTest {

    @Test
    public void metrikkerSendesTilSensuSocket() throws Exception {
        try (ServerSocket sensuServerSocketMock = new ServerSocket(0)) {
            sensuServerSocketMock.setSoTimeout(5000);
            setProperty(SENSU_CLIENT_PORT, Integer.toString(sensuServerSocketMock.getLocalPort()));

            RestUtils.withClient(c -> c.target("http://fasit.adeo.no/a/b/c").request().get());

            String sensuMetricMessage = IOUtils.toString(sensuServerSocketMock.accept().getInputStream(), Charset.defaultCharset());
            assertThat(sensuMetricMessage).isNotEmpty();
        }
    }


}