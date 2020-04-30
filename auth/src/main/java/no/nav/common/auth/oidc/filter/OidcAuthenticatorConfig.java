package no.nav.common.auth.oidc.filter;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.utils.AuthHeaderTokenFinder;
import no.nav.common.auth.utils.TokenFinder;

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

    // Name of the cookie where the users id token is stored (optional)
    // If provided: will try to retrieve id token from cookie before using "idTokenFinder"
    // If provided: will be used to set refreshed id tokens
    public String idTokenCookieName;

    // Retrieves the id token from incoming requests (optional)
    public TokenFinder idTokenFinder = new AuthHeaderTokenFinder();

    // Name of the cookie that the refresh token will be retrieved from (optional)
    public String refreshTokenCookieName;

    // Url to call when refreshing the id token (optional)
    public String refreshUrl;

    public boolean isValid() {
        return discoveryUrl != null
                && clientId != null
                && identType != null
                && idTokenFinder != null;
    }
}
