package no.nav.fo.apiapp.selftest;

import no.nav.fo.apiapp.JettyTest;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static no.nav.apiapp.ApiAppServletContextListener.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class SelfTestTest extends JettyTest {

    private PingableEksempel pingableEksempel;

    @Before
    public void setup() {
        pingableEksempel = getBean(PingableEksempel.class);
        pingableEksempel.setOk(true);
    }

    @Test
    public void selftest() {
        sjekkOKStatus(INTERNAL_SELFTEST);
        pingableEksempel.setOk(false);
        sjekkFeilStatus(INTERNAL_SELFTEST);
    }

    @Test
    public void selftestJson() {
        sjekkOKStatus(INTERNAL_SELFTEST_JSON);
        pingableEksempel.setOk(false);
        sjekkFeilStatus(INTERNAL_SELFTEST_JSON);
    }

    @Test
    public void isAlive() {
        sjekkOKStatus(INTERNAL_IS_ALIVE);
        assertThat(getString(INTERNAL_IS_ALIVE), equalTo("Application: UP"));
    }

    private void sjekkOKStatus(String path) {
        Response response = get(path);
        assertThat(response.getStatus(), is(200));
        assertThat(getString(path).toLowerCase(), not(containsString("error")));
    }

    private void sjekkFeilStatus(String path) {
        assertThat(getString(path).toLowerCase(), containsString("error"));
    }

}
