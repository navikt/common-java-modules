package no.nav.dialogarena.config.fasit;

import org.junit.Test;

import javax.net.ssl.SSLException;
import javax.ws.rs.NotAuthorizedException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.dialogarena.config.fasit.FasitUtils.*;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.FSS;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.SBS;
import static no.nav.dialogarena.config.fasit.TestEnvironment.Q6;
import static no.nav.dialogarena.config.fasit.TestEnvironment.T6;
import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isOneOf;
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
    public void getFSSLocal() {
        assertThat(FasitUtils.getFSSLocal("t6"), equalTo("test.local"));
        assertThat(FasitUtils.getFSSLocal("t"), equalTo("test.local"));
        assertThat(FasitUtils.getFSSLocal("q6"), equalTo("preprod.local"));
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
        OpenAmConfig openAmConfig = FasitUtils.getOpenAmConfig("t6");
        assertThat(openAmConfig.username, equalTo("srvOpenam_test"));
        assertThat(openAmConfig.password, not(nullValue()));

        assertThat(openAmConfig.logoutUrl, equalTo("https://tjenester-t6.nav.no/esso/logout"));
        assertThat(openAmConfig.restUrl, equalTo("https://itjenester-t6.oera.no/esso"));
    }

    @Test
    public void getTestUser() {
        TestUser serviceUser = FasitUtils.getTestUser("priveligert_veileder");
        assertThat(serviceUser.username, equalTo("Z990257"));
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
        DbCredentials dbCredentials = getDbCredentials(T6, "veilarbportefolje");
        assertEquals("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=d26dbfl020.test.local)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=VEILARBPORTEFOLJET6)(INSTANCE_NAME=cctf01)(UR=A)(SERVER=DEDICATED)))", dbCredentials.url);
        assertEquals("VEILARBPORTEFOLJE_T6", dbCredentials.username);
        assertNotNull(dbCredentials.password);
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

    public static void testServiceUserCertificate(ServiceUserCertificate certificate) {
        assertThat(certificate.getKeystorealias(), isOneOf("host-key", "app-key"));

        assertThat(certificate.getKeystorepassword(), notNullValue());
        assertThat(certificate.getKeystorepassword().length(), greaterThan(0));

        assertThat(certificate.getKeystore(), notNullValue());
        assertThat(certificate.getKeystore().length, greaterThan(1000));
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
    public void getDefaultDomain_() throws Exception {
        setTemporaryProperty(DEFAULT_ENVIRONMENT_VARIABLE_NAME, T6.toString(),()->{
            assertThat(getDefaultDomain(SBS),equalTo(OERA_T_LOCAL));
            assertThat(getDefaultDomain(FSS),equalTo(TEST_LOCAL));
        });

        setTemporaryProperty(DEFAULT_ENVIRONMENT_VARIABLE_NAME, Q6.toString(),()->{
            assertThat(getDefaultDomain(SBS),equalTo(OERA_Q_LOCAL));
            assertThat(getDefaultDomain(FSS),equalTo(PREPROD_LOCAL));
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

}