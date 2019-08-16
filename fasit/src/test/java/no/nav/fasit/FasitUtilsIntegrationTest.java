package no.nav.fasit;

import no.nav.fasit.dto.RestService;
import no.nav.sbl.dialogarena.test.FasitAssumption;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.NotAuthorizedException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import static no.nav.fasit.FasitUtils.*;
import static no.nav.fasit.FasitUtils.Zone.FSS;
import static no.nav.fasit.FasitUtils.Zone.SBS;
import static no.nav.fasit.TestEnvironment.Q6;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class FasitUtilsIntegrationTest {

    private String TEST_ENVIRONMENT = "q6";

    @Before
    public void assumeFasit() {
        FasitAssumption.assumeFasitAccessible();
        assumeFalse(FasitUtils.usingMock());
    }

    @Test
    public void resolveDomain_() throws Exception {
        assertThat(resolveDomain("feature", TEST_ENVIRONMENT), equalTo(PREPROD_LOCAL));
        assertThat(resolveDomain("veilarbaktivitet", TEST_ENVIRONMENT), equalTo(PREPROD_LOCAL));
        assertThat(resolveDomain("dittnav", "q1"), equalTo(OERA_Q_LOCAL));
    }

    @Test
    public void shouldReturnUsernamePasswordForDb() throws Exception {
        DbCredentials dbCredentials = getDbCredentials(Q6, "veilarbportefolje");
        assertEquals("jdbc:oracle:thin:@a01dbfl033.adeo.no:1521/VEILARBPORTEFOLJE_Q6", dbCredentials.url);
        assertEquals("VEILARBPORTEFOLJE", dbCredentials.username);
        assertNotNull(dbCredentials.password);
    }

    @Test
    public void getApplicationConfig() {
        ApplicationConfig dittnavApplicationConfig = FasitUtils.getApplicationConfig("dittnav", TEST_ENVIRONMENT);
        assertThat(dittnavApplicationConfig.domain, equalTo("oera-q.local"));

        ApplicationConfig situasjonApplicationConfig = FasitUtils.getApplicationConfig("veilarbaktivitet", TEST_ENVIRONMENT);
        assertThat(situasjonApplicationConfig.domain, equalTo("preprod.local"));
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
        Properties applicationEnvironment = FasitUtils.getApplicationEnvironment("dittnav");
        assertThat(applicationEnvironment.size(), greaterThan(10));

        Properties veilarbaktivitetEnvironment = FasitUtils.getApplicationEnvironment("dittnav", TEST_ENVIRONMENT);
        assertThat(veilarbaktivitetEnvironment.size(), greaterThan(10));

        Properties dittnavEnvironment = FasitUtils.getApplicationEnvironment("dittnav", "t1");
        assertThat(dittnavEnvironment.size(), greaterThan(10));
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
            FasitUtils.getServiceUser("srvveilarbaktivitet", "veilarbaktivitet", TEST_ENVIRONMENT);
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
        ServiceUser serviceUser = FasitUtils.getServiceUser("srvmodiaeventdistribution", "modiaeventdistribution", TEST_ENVIRONMENT);
        assertThat(serviceUser.username, equalTo("srvmodiaeventdistr"));
        assertThat(serviceUser.password, not(nullValue()));
    }

    @Test
    public void getOpenAmConfig() {
        assertThat(FasitUtils.getOpenAmConfig(), notNullValue());

        OpenAmConfig openAmConfig = FasitUtils.getOpenAmConfig(TEST_ENVIRONMENT);
        assertThat(openAmConfig.username, equalTo("deployer"));
        assertThat(openAmConfig.password, not(nullValue()));

        assertThat(openAmConfig.logoutUrl, equalTo(String.format("https://tjenester-%s.nav.no/esso/logout", TEST_ENVIRONMENT)));
        assertThat(openAmConfig.restUrl, equalTo(String.format("https://itjenester-%s.oera.no/esso", TEST_ENVIRONMENT)));
    }

    @Test
    public void getTestUser() {
        TestUser serviceUser = FasitUtils.getTestUser("priveligert_veileder");
        assertThat(serviceUser.username, equalTo("Z990336"));
        assertThat(serviceUser.password, not(nullValue()));

        TestUser serviceUserT4 = FasitUtils.getTestUser("privat_bruker", TEST_ENVIRONMENT);
        assertThat(serviceUserT4.username, equalTo("15076630302")); // Testbruker
        assertThat(serviceUserT4.password, not(nullValue()));
    }

    @Test
    public void getQCertificate() throws Exception {
        ServiceUserCertificate serviceUserCertificate = FasitUtils.getServiceUserCertificate("srvHenvendelse", "q");
        FasitUtilsTest.testServiceUserCertificate(serviceUserCertificate);
    }

    @Test
    public void getRestServices_() throws Exception {
        String alias = "veilarbjobbsokerkompetanseRS";
        List<RestService> restServices = getRestServices(alias);
        assertThat(restServices, Matchers.not(empty()));
        validateRestService(restServices.get(0), alias);
    }

    @Test
    public void getRestService_() throws Exception {
        String alias = "veilarbjobbsokerkompetanseRS";
        validateRestService(getRestService(alias), alias);
    }

    private void validateRestService(RestService restService, String alias) throws MalformedURLException {
        assertThat(restService.getAlias(), equalTo(alias));
        assertThat(restService.getEnvironment(), Matchers.not(isEmptyString()));
        assertThat(restService.getEnvironmentClass(), Matchers.not(isEmptyString()));
        assertThat(restService.getApplication(), Matchers.not(isEmptyString()));
        new URL(restService.getUrl());
    }

    @Test
    public void getRestServiceExists() {
        assertThat(FasitUtils.getRestServices("this.does.not.exist"), empty());
        assertThatThrownBy(() -> FasitUtils.getRestService("this.does.not.exist", "p"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("this.does.not.exist")
                .hasMessageContaining("p");

        assertThat(FasitUtils.getRestServices("fasit.rest.api"), not(empty()));
        assertThat(FasitUtils.getRestService("fasit.rest.api", "p").getUrl(), startsWith("https://fasit.adeo.no"));
    }

    @Test
    public void getLoadbalancerConfig_() {
        assertThat(FasitUtils.getLoadbalancerConfig("this.does.not.exist"), empty());
        assertThatThrownBy(() -> FasitUtils.getLoadbalancerConfig("this.does.not.exist", "p"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("this.does.not.exist")
                .hasMessageContaining("p");

        LoadBalancerConfig loadbalancerConfig = FasitUtils.getLoadbalancerConfig("loadbalancer:veilarbdemo", TEST_ENVIRONMENT);
        assertThat(loadbalancerConfig.environment, equalTo("q6"));
        assertThat(loadbalancerConfig.contextRoots, not(isEmptyOrNullString()));
        assertThat(loadbalancerConfig.url, not(isEmptyOrNullString()));
    }

    @Test
    public void getLdapConfig_() {
        LdapConfig ldapConfig = FasitUtils.getLdapConfig();
        assertThat(ldapConfig.url, not(isEmptyOrNullString()));
        assertThat(ldapConfig.username, not(isEmptyOrNullString()));
        assertThat(ldapConfig.password, not(isEmptyOrNullString()));
        assertThat(ldapConfig.baseDN, not(isEmptyOrNullString()));
    }

    @Test
    public void getWebServiceEndpoint_() {
        assertThat(FasitUtils.getWebServiceEndpoint("Aktoer_v2").url, not(isEmptyOrNullString()));
    }

    @Test
    public void getTestDataProperty_() {
        assertThat(FasitUtils.getTestDataProperty("sentral_enhet").get(),not(isEmptyOrNullString()));
        assertThat(FasitUtils.getTestDataProperty("not_existing_property").isPresent(),is(false));
    }

    @Test
    public void getQueues_() {
        validateQueue(FasitUtils.getQueue("VARSELPRODUKSJON.VARSLINGER"));

        List<Queue> queues = FasitUtils.getQueues("VARSELPRODUKSJON.VARSLINGER");
        assertThat(queues, not(empty()));
        queues.forEach(this::validateQueue);
    }

    @Test
    public void getQueueManagers_() {
        validateQueueManager(FasitUtils.getQueueManager("mqGateway03"));

        List<QueueManager> queueManagers = FasitUtils.getQueueManagers("mqGateway03");
        assertThat(queueManagers, not(empty()));
        queueManagers.forEach(this::validateQueueManager);
    }

    private void validateQueueManager(QueueManager queueManager) {
        assertThat(queueManager.getHostname(), not(isEmptyOrNullString()));
        assertThat(queueManager.getPort(), greaterThan(0));
        assertThat(queueManager.getName(), not(isEmptyOrNullString()));
        assertThat(queueManager.getEnvironmentClass(), not(isEmptyOrNullString()));
    }

    private void validateQueue(Queue queue) {
        assertThat(queue, notNullValue());
        assertThat(queue.getName(), not(isEmptyOrNullString()));
        assertThat(queue.getEnvironmentClass(), not(isEmptyOrNullString()));
    }

}
