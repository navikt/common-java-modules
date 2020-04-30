package no.nav.common.auth.utils;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(TokenUtils.expiresWithin(jwt, FIVE_MINUTES)).isTrue();
    }

    @Test
    public void expiresWithin__shouldReturnFalseIfNotExpiredAndNotExpiresSoon() {
        long expireAt = System.currentTimeMillis() + FIVE_MINUTES + 5000;
        JWT jwt = new PlainJWT(createClaimsSet(expireAt));

        assertThat(TokenUtils.expiresWithin(jwt, FIVE_MINUTES)).isFalse();
    }

    @Test
    public void expiresWithin__shouldReturnTrueIfExpiresSoon() {
        long expireAt = System.currentTimeMillis() + FIVE_MINUTES - 5000;
        JWT jwt = new PlainJWT(createClaimsSet(expireAt));

        assertThat(TokenUtils.expiresWithin(jwt, FIVE_MINUTES)).isTrue();
    }

}
