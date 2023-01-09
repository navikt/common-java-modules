package no.nav.common.token_client.utils;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;

import java.text.ParseException;
import java.util.Date;

public class TokenUtils {

    @SneakyThrows
    public static JWT parseJwtToken(String jwtToken) {
        return JWTParser.parse(jwtToken);
    }

    /**
     * Checks if JWT token has expired or will expire within {@code withinMillis}
     * @param jwt token that will be checked
     * @param withinMillis if the token expires within this time then it is regarded as expired
     * @return true if the token is expired or will expire within {@code withinMillis}, false otherwise
     */
    public static boolean expiresWithin(JWT jwt, long withinMillis) {
        if (jwt == null) {
            return true;
        }

        try {
            Date tokenExpiration = jwt.getJWTClaimsSet().getExpirationTime();

            // Token should have an expiration, but if it does not, then the safest option is to assume it to be expired
            if (tokenExpiration == null) {
                return true;
            }

            long expirationTime = tokenExpiration.getTime() - withinMillis;

            return System.currentTimeMillis() > expirationTime;
        } catch (ParseException e) {
            return true;
        }
    }

    public static String getSubject(String accessToken) {
        try {
            JWT token = JWTParser.parse(accessToken);

            String subject = token.getJWTClaimsSet().getSubject();

            if (subject == null) {
                throw new IllegalArgumentException("Unable to get subject, access token is missing subject");
            }

            return subject;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to get subject, access token is invalid");
        }
    }

}
