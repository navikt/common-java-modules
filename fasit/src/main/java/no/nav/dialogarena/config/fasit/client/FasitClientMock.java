package no.nav.dialogarena.config.fasit.client;

import no.nav.dialogarena.config.fasit.*;
import no.nav.dialogarena.config.fasit.dto.RestService;

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
                .build());
    }

    @Override
    public DbCredentials getDbCredentials(GetDbCredentialsRequest getDbCredentialsRequest) {
        throw new IllegalStateException();
    }

    @Override
    public ServiceUserCertificate getCertificate(GetCertificateRequest getCertificateRequest) {
        throw new IllegalStateException();
    }

    @Override
    public ServiceUser getCredentials(GetCredentialsRequest getCredentialsRequest) {
        return new ServiceUser()
                .setUsername("mock-user")
                .setPassword("mock-password");
    }

    @Override
    public ApplicationConfig getApplicationConfig(GetApplicationConfigRequest getApplicationConfigRequest) {
        throw new IllegalStateException();
    }

    @Override
    public Properties getApplicationEnvironment(GetApplicationEnvironmentRequest getApplicationEnvironmentRequest) {
        throw new IllegalStateException();
    }

    private String mockUrl(String alias) {
        return String.format("http://localhost:8080/mock/%s", alias);
    }

}

