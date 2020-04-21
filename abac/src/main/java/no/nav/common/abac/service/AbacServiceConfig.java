package no.nav.common.abac.service;

import lombok.Builder;
import lombok.Value;
import no.nav.common.abac.CredentialConstants;
import no.nav.common.utils.EnvironmentUtils;

import static no.nav.common.utils.EnvironmentUtils.*;

@Value
@Builder
public class AbacServiceConfig {

    public static final String ABAC_ENDPOINT_URL_PROPERTY_NAME = "ABAC_PDP_ENDPOINT_URL";

    private String username;
    private String password;
    private String endpointUrl;

    public static AbacServiceConfig readFromSystemVariables() {
        return AbacServiceConfig.builder()
                .username(getOptionalProperty(CredentialConstants.SYSTEMUSER_USERNAME).orElseGet(() -> EnvironmentUtils.getRequiredProperty(CredentialConstants.SYSTEMUSER_USERNAME, resolveSrvUserPropertyName())))
                .password(getOptionalProperty(CredentialConstants.SYSTEMUSER_PASSWORD).orElseGet(() -> EnvironmentUtils.getRequiredProperty(CredentialConstants.SYSTEMUSER_PASSWORD, resolverSrvPasswordPropertyName())))
                .endpointUrl(getRequiredProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME))
                .build();
    }

}
