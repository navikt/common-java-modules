package no.nav.brukerdialog.security.oidc.provider;

import lombok.Builder;
import lombok.Value;
import no.nav.brukerdialog.security.domain.IdentType;

import static no.nav.brukerdialog.security.Constants.AZUREADB2C_OIDC_COOKIE_NAME_FSS;
import static no.nav.brukerdialog.security.Constants.AZUREADB2C_OIDC_COOKIE_NAME_SBS;
import static no.nav.brukerdialog.security.domain.IdentType.EksternBruker;
import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Value
@Builder
public class AzureADB2CConfig {

    public static final String EXTERNAL_USERS_AZUREAD_B2C_DISCOVERY_URL = "AAD_B2C_DISCOVERY_URL";
    public static final String EXTERNAL_USERS_AZUREAD_B2C_EXPECTED_AUDIENCE = "AAD_B2C_CLIENTID_USERNAME";

    public static final String INTERNAL_USERS_AZUREAD_B2C_CLIENTID_PROPERTY_NAME = "LOGINSERVICE_OIDC_CLIENTID";
    public static final String INTERNAL_USERS_AZUREAD_B2C_CALLBACK_URI = "LOGINSERVICE_OIDC_CALLBACKURI";
    public static final String INTERNAL_USERS_AZUREAD_B2C_DISCOVERY_URI_PROPERTY_NAME = "LOGINSERVICE_OIDC_DISCOVERYURI";
    public static final String INTERNAL_USERS_AZUREAD_B2C_PASSWORD = "LOGINSERVICE_OIDC_PASSWORD";

    public static final String AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME_SKYA = "aad_b2c_discovery.url";
    public static final String AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME_SKYA = "aad_b2c_clientid.username";


    public final String discoveryUrl;
    public final String expectedAudience;
    public final String tokenName;
    public final IdentType identType;

    public static AzureADB2CConfig configureAzureAdForExternalUsers() {
        return AzureADB2CConfig.builder()
                .discoveryUrl(getRequiredProperty(EXTERNAL_USERS_AZUREAD_B2C_DISCOVERY_URL, AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME_SKYA))
                .expectedAudience(getRequiredProperty(EXTERNAL_USERS_AZUREAD_B2C_EXPECTED_AUDIENCE, AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME_SKYA))
                .identType(EksternBruker)
                .tokenName(AZUREADB2C_OIDC_COOKIE_NAME_SBS)
                .build();
    }

    public static AzureADB2CConfig configureAzureAdForInternalUsers() {
        return AzureADB2CConfig.builder()
                .discoveryUrl(getRequiredProperty(INTERNAL_USERS_AZUREAD_B2C_DISCOVERY_URI_PROPERTY_NAME))
                .expectedAudience(getRequiredProperty(INTERNAL_USERS_AZUREAD_B2C_CLIENTID_PROPERTY_NAME))
                .identType(InternBruker)
                .tokenName(AZUREADB2C_OIDC_COOKIE_NAME_FSS)
                .build();
    }
}
