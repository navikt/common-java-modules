package no.nav.fo.apiapp.selftest;

import no.nav.fo.apiapp.JettyTest;
import no.nav.sbl.dialogarena.common.abac.pep.AbacPersonId;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.RequestData;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Request;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.io.IOException;

import static no.nav.apiapp.ApiAppServletContextListener.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This test fails when run locally
 */
public class SelfTestTest extends JettyTest {

    private PingableEksempel pingableEksempel;

    @Before
    public void setup() throws IOException, NoSuchFieldException {
//        AbacService service = mock(AbacService.class);
//        when(service.askForPermission(any())).thenReturn(null);
//        addBean(service);
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
