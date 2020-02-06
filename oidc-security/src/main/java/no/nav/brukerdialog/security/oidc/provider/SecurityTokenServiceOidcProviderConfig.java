package no.nav.brukerdialog.security.oidc.provider;

import lombok.Builder;
import lombok.Value;
import no.nav.sbl.util.EnvironmentUtils;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Builder
@Value
public class SecurityTokenServiceOidcProviderConfig {

    public static final String STS_OIDC_CONFIGURATION_URL_PROPERTY = "SECURITY_TOKEN_SERVICE_OPENID_CONFIGURATION_URL";

    public final String discoveryUrl;

    public static SecurityTokenServiceOidcProviderConfig readFromSystemProperties() {
        return SecurityTokenServiceOidcProviderConfig.builder()
                .discoveryUrl(getRequiredProperty(STS_OIDC_CONFIGURATION_URL_PROPERTY))
                .build();
    }

}
