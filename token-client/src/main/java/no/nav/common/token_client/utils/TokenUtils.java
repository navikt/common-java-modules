package no.nav.common.token_client.utils;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.Base64;
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

    @SneakyThrows
    public static String hashToken(String token) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

}
