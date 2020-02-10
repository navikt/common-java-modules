package no.nav.common.oidc.auth;

import lombok.Value;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.oidc.OidcTokenValidator;
import no.nav.common.oidc.utils.TokenLocator;

@Value
public class OidcAuthenticator {

    public OidcTokenValidator tokenValidator;

    public TokenLocator tokenLocator;

    public IdentType identType;

    public String refreshUrl;

    public static OidcAuthenticator fromConfig(OidcAuthenticatorConfig config) {
        if (!config.isValid()) {
            throw new IllegalStateException("OidcAuthenticatorConfig is missing one or more values");
        }

        OidcTokenValidator validator = new OidcTokenValidator(config.discoveryUrl, config.clientId);
        TokenLocator locator = new TokenLocator(config.idTokenCookieName, config.refreshTokenCookieName);
        return new OidcAuthenticator(validator, locator, config.identType, config.refreshUrl);
    }

}
