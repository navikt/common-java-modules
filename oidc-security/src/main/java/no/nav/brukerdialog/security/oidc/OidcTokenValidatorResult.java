package no.nav.brukerdialog.security.oidc;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;

import java.util.Map;

import static java.util.Collections.emptyMap;

public class OidcTokenValidatorResult {
    private final boolean isValid;
    private final String errorMessage;
    private final String subject;
    private final long expSeconds;
    private final Map<String, Object> attributes;

    private OidcTokenValidatorResult(boolean isValid, String errorMessage, String subject, long expSeconds, Map<String, Object> attributes) {
        this.isValid = isValid;
        this.errorMessage = errorMessage;
        this.subject = subject;
        this.expSeconds = expSeconds;
        this.attributes = attributes;
    }

    public static OidcTokenValidatorResult invalid(String errorMessage) {
        return new OidcTokenValidatorResult(
                false,
                errorMessage,
                null,
                0,
                emptyMap()
        );
    }

    public static OidcTokenValidatorResult valid(JwtClaims claims) throws MalformedClaimException {
        return new OidcTokenValidatorResult(
                true,
                null,
                claims.getSubject(),
                claims.getExpirationTime().getValue(),
                claims.getClaimsMap()
        );
    }

    public boolean isValid() {
        return isValid;
    }

    public String getErrorMessage() {
        if (isValid) {
            throw new IllegalArgumentException("Can't get error message from valid token");
        }
        return errorMessage;
    }

    public String getSubject() {
        if (!isValid) {
            throw new IllegalArgumentException("Can't get claims from an invalid token");
        }
        return subject;
    }

    public long getExpSeconds() {
        if (!isValid) {
            throw new IllegalArgumentException("Can't get claims from an invalid token");
        }
        return expSeconds;
    }

    public Map<String, Object> getAttributes() {
        if (!isValid) {
            throw new IllegalArgumentException("Can't get claims from an invalid token");
        }
        return attributes;
    }

}
