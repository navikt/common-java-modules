package no.nav.common.token_client.utils;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TokenUtilsTest {

    private final static int FIVE_MINUTES = 1000 * 60 * 5;

    private JWTClaimsSet createClaimsSet(long exp) {
        return new JWTClaimsSet.Builder()
                .expirationTime(new Date(exp))
                .build();
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
