package no.nav.dialogarena.config.fasit;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class FasitUtilsTest {

    @Test
    public void getFasitPassword() {
        assertThat(FasitUtils.getFasitPassword(), not(nullValue()));
    }

    @Test
    public void getFasitUser() {
        assertThat(FasitUtils.getFasitUser(), not(nullValue()));
    }

    @Test
    public void getApplikasjonsKonfigurasjon() {
        ApplicationConfig aktivitetsplanApplicationConfig = FasitUtils.getApplicationConfig("aktivitetsplan", "t6");
        assertThat(aktivitetsplanApplicationConfig.domain, equalTo("oera-t.local"));

        ApplicationConfig situasjonApplicationConfig = FasitUtils.getApplicationConfig("veilarbsituasjon", "t6");
        assertThat(situasjonApplicationConfig.domain, equalTo("test.local"));
    }

    @Test
    public void getServiceBruker() {
        ServiceUser serviceUser = FasitUtils.getServiceUser("srvveilarbsituasjon", "veilarbsituasjon", "t6");
        assertThat(serviceUser.username, equalTo("srvveilarbsituasjon"));
        assertThat(serviceUser.password, not(nullValue()));
    }


}