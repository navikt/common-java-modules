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
    public void getApplicationConfig() {
        ApplicationConfig aktivitetsplanApplicationConfig = FasitUtils.getApplicationConfig("aktivitetsplan", "t6");
        assertThat(aktivitetsplanApplicationConfig.domain, equalTo("oera-t.local"));

        ApplicationConfig situasjonApplicationConfig = FasitUtils.getApplicationConfig("veilarbsituasjon", "t6");
        assertThat(situasjonApplicationConfig.domain, equalTo("test.local"));
    }

    @Test
    public void getServiceUser() {
        ServiceUser serviceUser = FasitUtils.getServiceUser("srvveilarbsituasjon", "veilarbsituasjon", "t6");
        assertThat(serviceUser.username, equalTo("srvveilarbsituasjon"));
        assertThat(serviceUser.password, not(nullValue()));
    }

    @Test
    public void getServiceUser_aliasDifferentFromUsername() {
        ServiceUser serviceUser = FasitUtils.getServiceUser("srvveilarbsituasjonproxy", "veilarbsituasjonproxy", "t6");
        assertThat(serviceUser.username, equalTo("srvveilarbsituasjo"));
        assertThat(serviceUser.password, not(nullValue()));
    }

    @Test
    public void getTestUser() {
        TestUser serviceUser = FasitUtils.getTestUser("priveligert_veileder");
        assertThat(serviceUser.username, equalTo("Z990281"));
        assertThat(serviceUser.password, not(nullValue()));
    }

    @Test
    public void getLdapConfig() {
        LdapConfig ldapConfig = FasitUtils.getLdapConfig("ldap","veilarbsituasjon", "t6");
        assertThat(ldapConfig.username, equalTo("srvSSOLinux"));
        assertThat(ldapConfig.password, not(nullValue()));
    }

}