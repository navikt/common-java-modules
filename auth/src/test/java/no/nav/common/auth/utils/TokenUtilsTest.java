package no.nav.common.auth.utils;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    public void expiresWithin__shouldReturnTrueIfExpirationNotSet() {
        JWT jwt = new PlainJWT(new JWTClaimsSet.Builder().build());

        assertThat(TokenUtils.expiresWithin(jwt, FIVE_MINUTES)).isTrue();
    }
}
