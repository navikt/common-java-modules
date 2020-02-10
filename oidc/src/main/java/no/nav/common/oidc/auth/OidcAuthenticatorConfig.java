package no.nav.common.oidc.auth;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import no.nav.brukerdialog.security.domain.IdentType;

@Wither
@NoArgsConstructor
@AllArgsConstructor
public class OidcAuthenticatorConfig {

    // OIDC discovery URL
    public String discoveryUrl;

    // Client ID / Audience
    public String clientId;

    // Name of the cookie where the id token should be stored
    public String idTokenCookieName;

    // What type of user is being authenticated
    public IdentType identType;

    // Name of the cookie where the refresh token should be stored (optional)
    public String refreshTokenCookieName;

    // Url to call when refreshing the id token (optional)
    public String refreshUrl;

    public boolean isValid() {
        return discoveryUrl != null
                && clientId != null
                && idTokenCookieName != null
                && identType != null;
    }
}
