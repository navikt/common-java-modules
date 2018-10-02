package no.nav.sbl.dialogarena.common.web.selftest.domain;

import no.nav.sbl.dialogarena.common.web.selftest.SelfTestStatus;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SelftestResultTest {

    @Test
    public void getResult__manglende_resultat_er_error(){
        SelftestResult selftestResult = SelftestResult.builder().build();
        assertThat(selftestResult.getResult()).isEqualTo(SelfTestStatus.ERROR);
    }

}