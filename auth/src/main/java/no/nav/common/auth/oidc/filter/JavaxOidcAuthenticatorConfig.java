package no.nav.common.auth.oidc.filter;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import no.nav.common.auth.context.UserRole;
import no.nav.common.auth.utils.JavaxAuthHeaderTokenFinder;
import no.nav.common.auth.utils.JavaxTokenFinder;

import java.util.Collections;
import java.util.List;

@Wither
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class JavaxOidcAuthenticatorConfig {

    // OIDC discovery URL
    public String discoveryUrl;

    // Is used to validate the audience claim (if the token has multiple audiences and the AZP claim is set, then AZP will also be validated against this list of IDs)
    public List<String> clientIds;

    // What type of user is being authenticated
    public UserRoleResolver userRoleResolver;

    // Name of the cookie where the users id token is stored (optional)
    // If provided: will try to retrieve id token from cookie before using "idTokenFinder"
    // If provided: will be used to set refreshed id tokens
    public String idTokenCookieName;

    // Retrieves the id token from incoming requests (optional)
    public JavaxTokenFinder idTokenFinder = new JavaxAuthHeaderTokenFinder();

    // Name of the cookie that the refresh token will be retrieved from (optional)
    public String refreshTokenCookieName;

    // Url to call when refreshing the id token (optional)
    public String refreshUrl;

    // Domain to use for the refreshed id token cookie (optional)
    public String refreshedCookieDomain;

    // Path to use for the refreshed id token cookie (optional)
    public String refreshedCookiePath;

    public boolean isValid() {
        return discoveryUrl != null
                && clientIds != null
                && userRoleResolver != null
                && idTokenFinder != null;
    }

    public JavaxOidcAuthenticatorConfig withClientId(String clientId) {
        this.clientIds = Collections.singletonList(clientId);
        return this;
    }

    public JavaxOidcAuthenticatorConfig withUserRole(UserRole userRole) {
        this.userRoleResolver = jwt -> userRole;
        return this;
    }

}
