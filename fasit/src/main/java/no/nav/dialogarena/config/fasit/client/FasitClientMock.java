package no.nav.dialogarena.config.fasit.client;

import no.nav.dialogarena.config.fasit.*;
import no.nav.dialogarena.config.fasit.dto.RestService;
import no.nav.sbl.util.EnvironmentUtils;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class FasitClientMock implements FasitClient {

    @Override
    public String getBaseUrl(GetBaseUrlRequest getBaseUrlRequest) {
        String baseUrlAlias = getBaseUrlRequest.baseUrlAlias;
        return mockUrl(baseUrlAlias);
    }

    @Override
    public OpenAmConfig getOpenAmConfig(String environment) {
        throw new IllegalStateException();
    }

    @Override
    public List<RestService> getRestServices(String alias) {
        return Collections.singletonList(RestService.builder()
                .url(mockUrl(alias))
                .environment(FasitUtils.getDefaultEnvironment())
                .build());
    }

    @Override
    public DbCredentials getDbCredentials(GetDbCredentialsRequest getDbCredentialsRequest) {
        String url = String.format("jdbc:h2:mem:%s-0;DB_CLOSE_DELAY=-1;MODE=Oracle;", getDbCredentialsRequest.applicationName);
        return new DbCredentials()
                .setUrl(url)
                .setUsername("mock-user-db")
                .setPassword("mock-password-db");
    }

    @Override
    public ServiceUserCertificate getCertificate(GetCertificateRequest getCertificateRequest) {
        return new ServiceUserCertificate()
                .setKeystore(new byte[0])
                .setKeystorealias(getCertificateRequest.alias)
                .setKeystorepassword("");
    }

    @Override
    public ServiceUser getCredentials(GetCredentialsRequest getCredentialsRequest) {
        String mockUserName = String.format("mock-user-%s", getCredentialsRequest.applicationName);
        return new ServiceUser()
                .setUsername(mockUserName)
                .setPassword("mock-password")
                .setEnvironment(getCredentialsRequest.environment)
                .setDomain(getCredentialsRequest.domain);
    }

    @Override
    public ApplicationConfig getApplicationConfig(GetApplicationConfigRequest getApplicationConfigRequest) {
        throw new IllegalStateException();
    }

    @Override
    public Properties getApplicationEnvironment(GetApplicationEnvironmentRequest getApplicationEnvironmentRequest) {
        return new Properties();
    }

    @Override
    public LdapConfig getLdapConfig(String environment) {
        throw new IllegalStateException();
    }

    @Override
    public List<Queue> getQueue(GetQueueRequest getQueueRequest) {
        return Collections.singletonList(new Queue().setName(getQueueRequest.alias));
    }

    @Override
    public List<QueueManager> getQueueManager(GetQueueManagerRequest getQueueManagerRequest) {
        return Collections.singletonList(new QueueManager()
                .setName(getQueueManagerRequest.alias)
                .setHostname("localhost")
                .setPort(7676)
        );
    }

    @Override
    public WebServiceEndpoint getWebServiceEndpoint(String alias, String environment) {
        throw new IllegalStateException();
    }

    @Override
    public List<LoadBalancerConfig> getLoadbalancerConfig(String alias) {
        throw new IllegalStateException();
    }

    @Override
    public List<ApplicationProperties> getApplicationProperties(GetApplicationPropertiesRequest getApplicationPropertiesRequest) {
        throw new IllegalStateException();
    }

    private String mockUrl(String alias) {
        return String.format("http://localhost:8080/mock/%s", alias);
    }

}

