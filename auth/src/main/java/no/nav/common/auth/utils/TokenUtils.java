package no.nav.common.auth.utils;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.UserRole;
import no.nav.common.auth.subject.IdentType;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;

@Slf4j
public class TokenUtils {

    public static IdentType mapUserRoleToIdentType(UserRole userRole) {
        switch (userRole) {
            case INTERN:
                return IdentType.InternBruker;
            case EKSTERN:
                return IdentType.EksternBruker;
            case SYSTEM:
                return IdentType.Systemressurs;
            default:
                throw new IllegalStateException("Unknown user role: " + userRole);
        }
    }

    public static Optional<String> getTokenFromHeader(HttpServletRequest request) {
        String headerValue = request.getHeader("Authorization");
        return headerValue != null && !headerValue.isEmpty() && headerValue.startsWith("Bearer ")
                ? Optional.of(headerValue.substring("Bearer ".length()))
                : Optional.empty();
    }

    public static String getUid(JWT token, UserRole userRole) throws ParseException {
        JWTClaimsSet claimsSet = token.getJWTClaimsSet();
        String subject = claimsSet.getSubject();

        if (userRole == UserRole.INTERN) {
            String navIdent = claimsSet.getStringClaim(AAD_NAV_IDENT_CLAIM);
            return navIdent != null
                    ? navIdent
                    : subject;
        }

        return subject;
    }

    public static boolean hasMatchingAudience(JWT jwtToken, List<String> audiences) {
        try {
            // Checks if any of the audiences in the token matches any of the given audiences
            List<String> tokenAudiences = jwtToken.getJWTClaimsSet().getAudience();
            return tokenAudiences.stream().anyMatch(audiences::contains);
        } catch (ParseException e) {
            return false;
        }
    }

    public static boolean hasMatchingIssuer(JWT jwt, String issuer) {
        try {
            return jwt.getJWTClaimsSet().getIssuer().equals(issuer);
        } catch (ParseException e) {
            return false;
        }
    }

    public static boolean isServiceUserToken(String oidcToken) {
        try {
            JWT jwt = JWTParser.parse(oidcToken);
            String subject = jwt.getJWTClaimsSet().getSubject();
            return subject.startsWith("srv");
        } catch (ParseException e) {
            log.error("Failed to parse token", e);
            throw new RuntimeException("Unable to verify service user. Failed to parse token");
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
