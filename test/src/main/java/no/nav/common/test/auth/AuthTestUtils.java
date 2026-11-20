package no.nav.common.test.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import java.util.Map;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.UserRole;

public class AuthTestUtils {

    public static final String TEST_AUDIENCE = "test-audience";

    public static final String TEST_ISSUER = "https://testIssuer.test";

    public static AuthContext createAuthContext(UserRole role, String subject) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .audience(TEST_AUDIENCE)
                .issuer(TEST_ISSUER)
                .build();

        return new AuthContext(role, new PlainJWT(claimsSet));
    }

    public static AuthContext createAuthContext(UserRole role, Map<String, Object> claims) {
        if (claims.get("sub") == null) {
            throw new IllegalArgumentException("The claim 'sub' is missing");
        }

        JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();
        claims.forEach(claimsSetBuilder::claim);

        if (claims.get("aud") == null) {
            claimsSetBuilder.claim("aud", TEST_AUDIENCE);
        }

        if (claims.get("iss") == null) {
            claimsSetBuilder.claim("iss", TEST_ISSUER);
        }

        JWTClaimsSet claimsSet = claimsSetBuilder.build();

        return new AuthContext(role, new PlainJWT(claimsSet));
    }
}
