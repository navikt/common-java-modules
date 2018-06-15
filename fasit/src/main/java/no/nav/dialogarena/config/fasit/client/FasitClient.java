package no.nav.dialogarena.config.fasit.client;

import lombok.Builder;
import lombok.Value;
import no.nav.dialogarena.config.fasit.*;
import no.nav.dialogarena.config.fasit.dto.RestService;

import java.util.List;
import java.util.Properties;

public interface FasitClient {
    String getBaseUrl(GetBaseUrlRequest getBaseUrlRequest);
    OpenAmConfig getOpenAmConfig(String environment);
    List<RestService> getRestServices(String alias);
    DbCredentials getDbCredentials(GetDbCredentialsRequest getDbCredentialsRequest);
    ServiceUserCertificate getCertificate(GetCertificateRequest getCertificateRequest);
    ServiceUser getCredentials(GetCredentialsRequest getCredentialsRequest);
    ApplicationConfig getApplicationConfig(GetApplicationConfigRequest getApplicationConfigRequest);
    Properties getApplicationEnvironment(GetApplicationEnvironmentRequest getApplicationEnvironmentRequest);


    @Builder
    @Value
    class GetBaseUrlRequest {
        public String environment;
        public String domain;
        public String baseUrlAlias;
        public String application;
    }

    @Builder
    @Value
    class GetDbCredentialsRequest {
        public String applicationName;
        public TestEnvironment testEnvironment;
    }

    @Builder
    @Value
    class GetCertificateRequest {
        public String environmentClass;
        public String alias;
    }

    @Builder
    @Value
    class GetCredentialsRequest {
        public String domain;
        public String environment;
        public String userAlias;
        public String applicationName;
    }

    @Builder
    @Value
    class GetApplicationConfigRequest {
        public String applicationName;
        public String environment;
    }

    @Builder
    @Value
    class GetApplicationEnvironmentRequest {
        public String applicationName;
        public String environment;
    }

}
