package no.nav.dialogarena.config.fasit;

import no.nav.dialogarena.config.fasit.dto.RestService;
import no.nav.sbl.dialogarena.test.FasitAssumption;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.ws.rs.NotAuthorizedException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.dialogarena.config.fasit.FasitUtils.*;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.FSS;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.SBS;
import static no.nav.dialogarena.config.fasit.FasitUtilsTest.testServiceUserCertificate;
import static no.nav.dialogarena.config.fasit.TestEnvironment.T6;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.*;

public class FasitUtilsIntegrationTest {

    @Before
    public void assumeFasit() {
        FasitAssumption.assumeFasitAccessible();
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
    public void resolveDomain_() throws Exception {
        assertThat(resolveDomain("feature", "t6"), equalTo(TEST_LOCAL));
        assertThat(resolveDomain("feature", "q6"), equalTo(PREPROD_LOCAL));
        assertThat(resolveDomain("veilarbaktivitet", "t6"), equalTo(TEST_LOCAL));
        assertThat(resolveDomain("veilarbaktivitet", "q6"), equalTo(PREPROD_LOCAL));

        assertThat(resolveDomain("aktivitetsplan", "t6"), equalTo(OERA_T_LOCAL));
        assertThat(resolveDomain("aktivitetsplan", "q6"), equalTo(OERA_Q_LOCAL));
    }

    @Test
    public void shouldReturnUsernamePasswordForDb() throws Exception {
        DbCredentials dbCredentials = getDbCredentials(T6, "veilarbportefolje");
        assertEquals("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=d26dbfl020.test.local)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=VEILARBPORTEFOLJET6)(INSTANCE_NAME=cctf01)(UR=A)(SERVER=DEDICATED)))", dbCredentials.url);
        assertEquals("VEILARBPORTEFOLJE_T6", dbCredentials.username);
        assertNotNull(dbCredentials.password);
    }

    @Test
    public void getApplicationConfig() {
        ApplicationConfig aktivitetsplanApplicationConfig = FasitUtils.getApplicationConfig("aktivitetsplan", "t6");
        assertThat(aktivitetsplanApplicationConfig.domain, equalTo("oera-t.local"));

        ApplicationConfig situasjonApplicationConfig = FasitUtils.getApplicationConfig("veilarbaktivitet", "t6");
        assertThat(situasjonApplicationConfig.domain, equalTo("test.local"));
    }

    @Test
    public void getServiceUser() {
        ServiceUser serviceUser = FasitUtils.getServiceUser("srvveilarbaktivitet", "veilarbaktivitet");
        assertThat(serviceUser.username, equalTo("srvveilarbaktivitet"));
        assertThat(serviceUser.password, not(nullValue()));
    }

    @Test
    public void getServiceUser_nais_app() {
        ServiceUser serviceUser = FasitUtils.getServiceUser(
                "srvveilarbdemo",
                "veilarbdemo"
        );
        assertThat(serviceUser.username, equalTo("srvveilarbdemo"));
        assertThat(serviceUser.password, not(nullValue()));
    }

    @Test
    public void getApplicationEnvironment() throws Exception {
        Properties applicationEnvironment = FasitUtils.getApplicationEnvironment("veilarbdialog");
        assertThat(applicationEnvironment.size(), greaterThan(10));

        Properties veilarbaktivitetEnvironment = FasitUtils.getApplicationEnvironment("veilarbaktivitet", "t6");
        assertThat(veilarbaktivitetEnvironment.size(), greaterThan(10));

        Properties veilarbaktivitetproxyEnvironment = FasitUtils.getApplicationEnvironment("veilarbaktivitetproxy", "t6");
        assertThat(veilarbaktivitetproxyEnvironment.size(), greaterThan(10));
    }

    @Test
    public void getBaseUrl_() throws Exception {
        new URL(getBaseUrl("bekkci_slack_webhook_url"));
        new URL(getBaseUrl("securityTokenService"));
        new URL(getBaseUrl("securityTokenService", FSS));
        new URL(getBaseUrl("securityTokenService", SBS));
    }

    @Test
    public void getFasitPassword() {
        assertThat(FasitUtils.getFasitPassword(), not(nullValue()));
    }

    @Test
    public void getFasitUser() {
        assertThat(FasitUtils.getFasitUser(), not(nullValue()));
    }

    @Test(expected = NotAuthorizedException.class)
    public void getServiceUser_invalidFasitPassword() {
        String property = System.getProperty(FASIT_USERNAME_VARIABLE_NAME);
        try {
            System.setProperty(FASIT_USERNAME_VARIABLE_NAME, "invalidpassword");
            FasitUtils.getServiceUser("srvveilarbaktivitet", "veilarbaktivitet", "t6");
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
        ServiceUser serviceUser = FasitUtils.getServiceUser("srvveilarbaktivitetproxy", "veilarbaktivitetproxy", "t6");
        assertThat(serviceUser.username, equalTo("srvveilarbaktivite"));
        assertThat(serviceUser.password, not(nullValue()));
    }

    @Test
    public void getOpenAmConfig() {
        assertThat(FasitUtils.getOpenAmConfig(), notNullValue());

        OpenAmConfig openAmConfig = FasitUtils.getOpenAmConfig("t6");
        assertThat(openAmConfig.username, equalTo("srvOpenam_test"));
        assertThat(openAmConfig.password, not(nullValue()));

        assertThat(openAmConfig.logoutUrl, equalTo("https://tjenester-t6.nav.no/esso/logout"));
        assertThat(openAmConfig.restUrl, equalTo("https://itjenester-t6.oera.no/esso"));
    }

    @Test
    public void getTestUser() {
        TestUser serviceUser = FasitUtils.getTestUser("priveligert_veileder");
        assertThat(serviceUser.username, equalTo("Z991683"));
        assertThat(serviceUser.password, not(nullValue()));

        TestUser serviceUserT4 = FasitUtils.getTestUser("privat_bruker", "t4");
        assertThat(serviceUserT4.username, equalTo("10108000398"));
        assertThat(serviceUserT4.password, not(nullValue()));
    }

    @Test
    public void getLdapConfig() {
        LdapConfig ldapConfig = FasitUtils.getLdapConfig("ldap", "veilarbaktivitet", "t6");
        assertThat(ldapConfig.username, equalTo("srvSSOLinux"));
        assertThat(ldapConfig.password, not(nullValue()));
    }

    @Test
    public void getTCertificate() throws Exception {
        ServiceUserCertificate serviceUserCertificate = FasitUtils.getServiceUserCertificate("srvHenvendelse", "t");
        testServiceUserCertificate(serviceUserCertificate);
    }

    @Test
    public void getQCertificate() throws Exception {
        ServiceUserCertificate serviceUserCertificate = FasitUtils.getServiceUserCertificate("srvHenvendelse", "q");
        testServiceUserCertificate(serviceUserCertificate);
    }

    @Test(expected = NotAuthorizedException.class)
    public void getPCertificate() throws Exception {
        FasitUtils.getServiceUserCertificate("srvHenvendelse", "p");
    }

    @Test
    public void getRestService_() throws Exception {
        String alias = "veilarbjobbsokerkompetanseRS";
        List<RestService> restServices = getRestService(alias);
        assertThat(restServices, Matchers.not(empty()));

        RestService restService = restServices.get(0);
        assertThat(restService.getAlias(), equalTo(alias));
        assertThat(restService.getEnvironment(), Matchers.not(isEmptyString()));
        assertThat(restService.getEnvironmentClass(), Matchers.not(isEmptyString()));
        assertThat(restService.getApplication(), Matchers.not(isEmptyString()));
        new URL(restService.getUrl());
    }

}
