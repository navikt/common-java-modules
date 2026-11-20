package no.nav.common.token_client.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class TokenUtilsTest {

    private static final int FIVE_MINUTES = 1000 * 60 * 5;

    private JWTClaimsSet createClaimsSet(long exp) {
        return new JWTClaimsSet.Builder().expirationTime(new Date(exp)).build();
    }

    @Test
    public void expiresWithin__shouldReturnTrueIfExpired() {
        long expireAt = System.currentTimeMillis() - 5000;
        JWT jwt = new PlainJWT(createClaimsSet(expireAt));

        assertTrue(TokenUtils.expiresWithin(jwt, FIVE_MINUTES));
    }

    @Test
    public void expiresWithin__shouldReturnFalseIfNotExpiredAndNotExpiresSoon() {
        long expireAt = System.currentTimeMillis() + FIVE_MINUTES + 5000;
        JWT jwt = new PlainJWT(createClaimsSet(expireAt));

        assertFalse(TokenUtils.expiresWithin(jwt, FIVE_MINUTES));
    }

    @Test
    public void expiresWithin__shouldReturnTrueIfExpiresSoon() {
        long expireAt = System.currentTimeMillis() + FIVE_MINUTES - 5000;
        JWT jwt = new PlainJWT(createClaimsSet(expireAt));

        assertTrue(TokenUtils.expiresWithin(jwt, FIVE_MINUTES));
    }

    @Test
    public void expiresWithin__shouldReturnTrueIfExpirationNotSet() {
        JWT jwt = new PlainJWT(new JWTClaimsSet.Builder().build());

        assertTrue(TokenUtils.expiresWithin(jwt, FIVE_MINUTES));
    }
}
