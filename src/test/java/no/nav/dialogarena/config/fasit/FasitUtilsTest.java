package no.nav.dialogarena.config.fasit;

import org.junit.Test;

import javax.net.ssl.SSLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.dialogarena.config.fasit.FasitUtils.FASIT_USERNAME_VARIABLE_NAME;
import static no.nav.dialogarena.config.fasit.FasitUtils.getDbCredentials;
import static no.nav.dialogarena.config.fasit.TestEnvironment.T6;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class FasitUtilsTest {

    @Test
    public void getEnvironmentClass_() {
        assertThat(FasitUtils.getEnvironmentClass("t6"), equalTo("t"));
        assertThat(FasitUtils.getEnvironmentClass("t"), equalTo("t"));
        assertThat(FasitUtils.getEnvironmentClass("q6"), equalTo("q"));
    }

    @Test
    public void getOeraLocal_() {
        assertThat(FasitUtils.getOeraLocal("t6"), equalTo("oera-t.local"));
        assertThat(FasitUtils.getOeraLocal("t"), equalTo("oera-t.local"));
        assertThat(FasitUtils.getOeraLocal("q6"), equalTo("oera-q.local"));
    }

    @Test
    public void erEksterntDomene_() {
        assertThat(FasitUtils.erEksterntDomene("adeo.no"), is(false));
        assertThat(FasitUtils.erEksterntDomene("test.local"), is(false));
        assertThat(FasitUtils.erEksterntDomene("devillo.no"), is(false));
        assertThat(FasitUtils.erEksterntDomene("preprod.local"), is(false));

        assertThat(FasitUtils.erEksterntDomene("oera.no"), is(true));
        assertThat(FasitUtils.erEksterntDomene("oera-q.local"), is(true));
        assertThat(FasitUtils.erEksterntDomene("oera-t.local"), is(true));
    }

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

    @Test(expected = IllegalStateException.class)
    public void getServiceUser_invalidFasitPassword() {
        String property = System.getProperty(FASIT_USERNAME_VARIABLE_NAME);
        try {
            System.setProperty(FASIT_USERNAME_VARIABLE_NAME, "invalidpassword");
            FasitUtils.getServiceUser("srvveilarbsituasjon", "veilarbsituasjon", "t6");
        } finally {
            if (property != null) {
                System.setProperty(FASIT_USERNAME_VARIABLE_NAME, property);
            } else {
                System.clearProperty(FASIT_USERNAME_VARIABLE_NAME);
            }
        }
    }

    @Test
    public void getServiceUser_aliasDifferentFromUsername() {
        ServiceUser serviceUser = FasitUtils.getServiceUser("srvveilarbsituasjonproxy", "veilarbsituasjonproxy", "t6");
        assertThat(serviceUser.username, equalTo("srvveilarbsituasjo"));
        assertThat(serviceUser.password, not(nullValue()));
    }

    @Test
    public void getOpenAmConfig() {
        OpenAmConfig openAmConfig = FasitUtils.getOpenAmConfig("t6");
        assertThat(openAmConfig.username, equalTo("srvOpenam_test"));
        assertThat(openAmConfig.password, not(nullValue()));

        assertThat(openAmConfig.logoutUrl, equalTo("https://tjenester-t6.nav.no/esso/logout"));
        assertThat(openAmConfig.restUrl, equalTo("https://itjenester-t6.oera.no/esso"));
    }

    @Test
    public void getTestUser() {
        TestUser serviceUser = FasitUtils.getTestUser("priveligert_veileder");
        assertThat(serviceUser.username, equalTo("Z990761"));
        assertThat(serviceUser.password, not(nullValue()));
    }

    @Test
    public void getLdapConfig() {
        LdapConfig ldapConfig = FasitUtils.getLdapConfig("ldap", "veilarbsituasjon", "t6");
        assertThat(ldapConfig.username, equalTo("srvSSOLinux"));
        assertThat(ldapConfig.password, not(nullValue()));
    }

    @Test
    public void httpClient() {
        AtomicBoolean hasFailed = new AtomicBoolean();
        FasitUtils.httpClient((c) -> {
            if (hasFailed.get()) {
                return "ok!";
            } else {
                hasFailed.set(true);
                throw new SSLException("handshake_failure");
            }
        });
    }

    @Test
    public void shouldReturnUsernamePasswordForDb() throws Exception {
        String expectedUsername = "VEILARBPORTEFOLJE_T6";
        FasitUtils.UsernameAndPassword actual = getDbCredentials(T6, "veilarbportefolje");
        assertEquals(expectedUsername, actual.username);
        assertNotNull(actual.password);
    }
}