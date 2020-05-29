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

    @Test
    public void harFeil__ok_er_ikke_feil(){
        SelftestResult selftestResult = SelftestResult.builder().result(SelfTestStatus.OK).build();
        assertThat(selftestResult.harFeil()).isFalse();
    }

    @Test
    public void harFeil__warning_er_feil(){
        SelftestResult selftestResult = SelftestResult.builder().result(SelfTestStatus.WARNING).build();
        assertThat(selftestResult.harFeil()).isTrue();
    }

    @Test
    public void harFeil__disabled_er_ikke_feil(){
        SelftestResult selftestResult = SelftestResult.builder().result(SelfTestStatus.DISABLED).build();
        assertThat(selftestResult.harFeil()).isFalse();
    }

    @Test
    public void harFeil__error_er_feil(){
        SelftestResult selftestResult = SelftestResult.builder().result(SelfTestStatus.ERROR).build();
        assertThat(selftestResult.harFeil()).isTrue();
    }

}
