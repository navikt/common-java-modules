package no.nav.common.oidc.auth;

import lombok.Value;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.oidc.OidcTokenValidator;
import no.nav.common.oidc.utils.TokenLocator;

@Value
public class OidcAuthenticator {

    public OidcTokenValidator tokenValidator;

    public TokenLocator idTokenLocator;

    public IdentType identType;

    public static OidcAuthenticator fromConfig(OidcAuthenticatorConfig config) {
        OidcTokenValidator validator = new OidcTokenValidator(config.discoveryUrl, config.clientId);
        TokenLocator locator = new TokenLocator(config.idTokenCookieName);
        return new OidcAuthenticator(validator, locator, config.identType);
    }

}
