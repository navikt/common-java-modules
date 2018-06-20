package no.nav.sbl.dialogarena.common.abac.pep.service;

import lombok.Builder;
import lombok.Value;

import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_PASSWORD;
import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_USERNAME;
import static no.nav.sbl.util.EnvironmentUtils.*;

@Value
@Builder
public class AbacServiceConfig {

    public static final String ABAC_ENDPOINT_URL_PROPERTY_NAME_SKYA = "abac.endpoint.url";
    public static final String ABAC_ENDPOINT_URL_PROPERTY_NAME = "ABAC_PDP_ENDPOINT_URL";

    private String username;
    private String password;
    private String endpointUrl;

    public static AbacServiceConfig readFromSystemVariables() {
        return AbacServiceConfig.builder()
                .username(getOptionalProperty(SYSTEMUSER_USERNAME).orElseGet(() -> getRequiredProperty(SYSTEMUSER_USERNAME, resolveSrvUserPropertyName())))
                .password(getOptionalProperty(SYSTEMUSER_PASSWORD).orElseGet(() -> getRequiredProperty(SYSTEMUSER_PASSWORD, resolverSrvPasswordPropertyName())))
                .endpointUrl(getRequiredProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME, ABAC_ENDPOINT_URL_PROPERTY_NAME_SKYA))
                .build();
    }

}
