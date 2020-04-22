package no.nav.common.oidc.utils;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.auth.IdentType;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

import static no.nav.common.oidc.Constants.AAD_NAV_IDENT_CLAIM;

public class TokenUtils {

    public static Optional<String> getTokenFromHeader(HttpServletRequest request) {
        String headerValue = request.getHeader("Authorization");
        return headerValue != null && !headerValue.isEmpty() && headerValue.startsWith("Bearer ")
                ? Optional.of(headerValue.substring("Bearer ".length()))
                : Optional.empty();
    }

    public static String getUid(JWT token, IdentType identType) throws ParseException {
        JWTClaimsSet claimsSet = token.getJWTClaimsSet();
        String subject = claimsSet.getSubject();

        if (identType == IdentType.InternBruker) {
            String navIdent = claimsSet.getStringClaim(AAD_NAV_IDENT_CLAIM);
            return navIdent != null
                    ? navIdent
                    : subject;
        }

        return subject;
    }

    public static boolean hasMatchingIssuer(JWT jwt, String issuer) {
        try {
            return jwt.getJWTClaimsSet().getIssuer().equals(issuer);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Checks if JWT token has expired or will expire within {@code withinMillis}
     * @param jwt token that will be checked
     * @param withinMillis if the token expires within this time then it is regarded as expired
     * @return true if the token is expired or will expire within {@code withinMillis}, false otherwise
     */
    public static boolean expiresWithin(JWT jwt, long withinMillis) {
        try {
            Date tokenExpiration = jwt.getJWTClaimsSet().getExpirationTime();
            long expirationTime = tokenExpiration.getTime() - withinMillis;

            return System.currentTimeMillis() > expirationTime;
        } catch (ParseException e) {
            return true;
        }
    }

}
