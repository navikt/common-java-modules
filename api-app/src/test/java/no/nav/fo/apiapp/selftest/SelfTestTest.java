package no.nav.fo.apiapp.selftest;

import no.nav.fo.apiapp.JettyTest;
import no.nav.sbl.dialogarena.test.junit.VirkerIkkeLokaltCategory;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.core.Response;

import static no.nav.apiapp.ApiAppServletContextListener.*;
import static org.assertj.core.api.Assertions.assertThat;


@Category(VirkerIkkeLokaltCategory.class)
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
        sjekkFeilmelding(INTERNAL_SELFTEST);
    }

    @Test
    public void isReady() {
        pingableEksempel.setOk(false);
        sjekkFeilstatus(INTERNAL_IS_READY);
        pingableEksempel.setOk(true);
        sjekkOKStatus(INTERNAL_IS_READY);
        pingableEksempel.setOk(false);
        sjekkOKStatus(INTERNAL_IS_READY);
    }

    @Test
    public void isAlive() {
        sjekkOKStatus(INTERNAL_IS_ALIVE);
        assertThat(getString(INTERNAL_IS_ALIVE)).isEqualTo("Application: UP");
    }

    private void sjekkOKStatus(String path) {
        Response response = get(path);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(getString(path).toLowerCase()).doesNotContain(">error<");
    }

    private void sjekkFeilmelding(String path) {
        assertThat(getString(path).toLowerCase()).contains(">error<");
    }

    private void sjekkFeilstatus(String path) {
        assertThat(get(path).getStatus()).isBetween(500, 599);
    }

}
