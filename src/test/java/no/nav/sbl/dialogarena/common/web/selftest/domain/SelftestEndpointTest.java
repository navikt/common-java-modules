package no.nav.sbl.dialogarena.common.web.selftest.domain;

import org.junit.Test;

import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet.STATUS_ERROR;
import static org.assertj.core.api.Assertions.assertThat;

public class SelftestEndpointTest {

    @Test
    public void getResult__manglende_resultat_er_error(){
        assertThat(new SelftestEndpoint().getResult()).isEqualTo(STATUS_ERROR);
    }

}