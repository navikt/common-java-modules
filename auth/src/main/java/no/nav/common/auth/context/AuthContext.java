package no.nav.common.auth.context;

import com.nimbusds.jwt.JWT;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Represents the OIDC authentication context for the requesting user
 */
@Value
@AllArgsConstructor
public class AuthContext {

    @NonNull
    UserRole role;

    @NonNull
    JWT idToken;

    String accessToken;

    public AuthContext(UserRole role, JWT idToken) {
        this.role = role;
        this.idToken = idToken;
        this.accessToken = null;
    }

}
