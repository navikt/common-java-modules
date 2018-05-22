package no.nav.sbl.dialogarena.common.abac.pep.service;

import lombok.Builder;
import lombok.Value;

import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_PASSWORD;
import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_USERNAME;
import static no.nav.sbl.dialogarena.common.abac.pep.Utils.getApplicationProperty;

@Value
@Builder
public class AbacServiceConfig {

    public static final String ABAC_ENDPOINT_URL_PROPERTY_NAME = "abac.endpoint.url";

    private String username;
    private String password;
    private String endpointUrl;

    public static AbacServiceConfig readFromSystemVariables() {
        return AbacServiceConfig.builder()
                .username(getApplicationProperty(SYSTEMUSER_USERNAME))
                .password(getApplicationProperty(SYSTEMUSER_PASSWORD))
                .endpointUrl(getApplicationProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME))
                .build();
    }

}
