package no.nav.common.health.domain;

import no.nav.common.health.SelfTestStatus;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SelftestResultTest {

    @Test
    public void getResult__manglende_resultat_er_error(){
        SelftestResult selftestResult = SelftestResult.builder().build();
        assertThat(selftestResult.getResult()).isEqualTo(SelfTestStatus.ERROR);
    }

}
