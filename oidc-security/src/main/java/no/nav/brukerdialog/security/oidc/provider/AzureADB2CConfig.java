package no.nav.brukerdialog.security.oidc.provider;

import lombok.Builder;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.StringUtils.assertNotNullOrEmpty;

@Builder
public class AzureADB2CConfig {

    public static final String AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME = "AAD_B2C_DISCOVERY_URL";
    public static final String AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME = "AAD_B2C_CLIENTID_USERNAME";

    public static final String AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME_SKYA = "aad_b2c_discovery.url";
    public static final String AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME_SKYA = "aad_b2c_clientid.username";


    public final String discoveryUrl;
    public final String expectedAudience;

    private AzureADB2CConfig(String discoveryUrl, String expectedAudience) {
        this.discoveryUrl = assertNotNullOrEmpty(discoveryUrl);
        this.expectedAudience = expectedAudience;
    }

    public static AzureADB2CConfig readFromSystemProperties() {
        return AzureADB2CConfig.builder()
                .discoveryUrl(getRequiredProperty(AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME, AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME_SKYA))
                .expectedAudience(getRequiredProperty(AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME, AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME_SKYA))
                .build();
    }

}
