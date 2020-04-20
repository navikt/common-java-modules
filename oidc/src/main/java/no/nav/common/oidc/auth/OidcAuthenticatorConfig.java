package no.nav.common.oidc.auth;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.oidc.utils.TokenFinder;

@Wither
@NoArgsConstructor
@AllArgsConstructor
public class OidcAuthenticatorConfig {

    // OIDC discovery URL
    public String discoveryUrl;

    // Client ID / Audience
    public String clientId;

    // What type of user is being authenticated
    public IdentType identType;

    // Retrieves the id token from incoming requests
    public TokenFinder idTokenFinder;

    // Name of the cookie where the id token should be stored when refreshing (optional)
    public String refreshIdTokenCookieName;

    // Retrieves the refresh token from cookie (optional)
    public TokenFinder refreshTokenFinder;

    // Url to call when refreshing the id token (optional)
    public String refreshUrl;

    public boolean isValid() {
        return discoveryUrl != null
                && clientId != null
                && identType != null
                && idTokenFinder != null;
    }
}
