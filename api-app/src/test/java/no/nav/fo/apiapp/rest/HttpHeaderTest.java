package no.nav.fo.apiapp.rest;

import no.nav.fo.apiapp.JettyTest;
import no.nav.fo.apiapp.JettyTestConfig;
import org.junit.After;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpHeaderTest extends JettyTest{

    private JettyTest jettyTest;

    @After
    public void stoppJetty() {
        jettyTest.stopJetty();
    }

    @Test
    public void skal_inneholde_pragma_header() {

        JettyTestConfig config = JettyTestConfig.builder()
                .disablePragmaHeader(false)
                .build();

        jettyTest = new JettyTest(config).startJetty();

        Response response = get("/api/eksempel");
        sjekkStatus(response,OK);
        assertThat(response.getHeaderString("Pragma")).isEqualToIgnoringCase("no-cache");
    }

    @Test
    public void skal_fjerne_pragma_header() {

        JettyTestConfig config = JettyTestConfig.builder()
                .disablePragmaHeader(true)
                .build();

        jettyTest = new JettyTest(config).startJetty();

        Response response = get("/api/eksempel");
        sjekkStatus(response,OK);
        assertThat(response.getHeaderString("Pragma")).isNullOrEmpty();
    }

    @Test
    public void skal_tillate_lagring_paa_klientsiden() {

        JettyTestConfig config = JettyTestConfig.builder()
                .allowClientStorage(true)
                .build();

        jettyTest = new JettyTest(config).startJetty();

        Response response = get("/api/eksempel");
        sjekkStatus(response,OK);
        assertThat(response.getHeaderString(CACHE_CONTROL)).isEqualToIgnoringCase("no-cache");
    }

    private void sjekkStatus(Response response, Response.Status status) {
        assertThat(response.getStatus()).isEqualTo(status.getStatusCode());
    }
}
