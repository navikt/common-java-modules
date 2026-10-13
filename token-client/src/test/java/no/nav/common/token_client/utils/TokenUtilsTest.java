package no.nav.common.token_client.utils;

import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TokenUtilsTest {

    private final static int FIVE_MINUTES = 1000 * 60 * 5;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-05T07:00:00Z"), ZoneOffset.UTC);

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

    private JWT tokenExpiringIn(long seconds) {
        return new PlainJWT(createClaimsSet(clock.instant().plusSeconds(seconds).toEpochMilli()));
    }

    @Test
    public void getCachedTokenTtl__shouldSubtractSafetyMargin() {
        assertEquals(Duration.ofSeconds(90),
                TokenUtils.getCachedTokenTtl(tokenExpiringIn(120), Duration.ofSeconds(30), clock));
    }

    @Test
    public void getCachedTokenTtl__shouldUseActualExpirationWithoutOneHourCap() {
        assertEquals(Duration.ofSeconds(7170),
                TokenUtils.getCachedTokenTtl(tokenExpiringIn(7200), Duration.ofSeconds(30), clock));
    }

    @Test
    public void getCachedTokenTtl__shouldReturnZeroForExpiredToken() {
        assertEquals(Duration.ZERO,
                TokenUtils.getCachedTokenTtl(tokenExpiringIn(-1), Duration.ofSeconds(30), clock));
    }

    @Test
    public void getCachedTokenTtl__shouldReturnZeroWithinMargin() {
        assertEquals(Duration.ZERO,
                TokenUtils.getCachedTokenTtl(tokenExpiringIn(29), Duration.ofSeconds(30), clock));
    }

    @Test
    public void getCachedTokenTtl__shouldReturnZeroAtMarginBoundary() {
        assertEquals(Duration.ZERO,
                TokenUtils.getCachedTokenTtl(tokenExpiringIn(30), Duration.ofSeconds(30), clock));
    }

    @Test
    public void getCachedTokenTtl__shouldAllowZeroMargin() {
        assertEquals(Duration.ofSeconds(120),
                TokenUtils.getCachedTokenTtl(tokenExpiringIn(120), Duration.ZERO, clock));
    }

    @Test
    public void getCachedTokenTtl__shouldRejectNegativeMargin() {
        assertThrows(IllegalArgumentException.class,
                () -> TokenUtils.getCachedTokenTtl(tokenExpiringIn(120), Duration.ofSeconds(-1), clock));
    }

    @Test
    public void getCachedTokenTtl__shouldRejectMissingExpiration() {
        JWT jwt = new PlainJWT(new JWTClaimsSet.Builder().build());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> TokenUtils.getCachedTokenTtl(jwt, Duration.ofSeconds(30), clock));
        assertEquals("Token expiration is required", exception.getMessage());
    }

    @Test
    public void getCachedTokenTtl__shouldRejectInvalidClaimsWithoutIncludingToken() {
        String header = Base64URL.encode("{\"alg\":\"none\"}").toString();
        String payload = Base64URL.encode("{\"exp\":\"invalid\",\"sub\":\"sensitive-subject\"}").toString();
        JWT jwt = TokenUtils.parseJwtToken(header + "." + payload + ".");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> TokenUtils.getCachedTokenTtl(jwt, Duration.ofSeconds(30), clock));
        assertEquals("Token expiration could not be read", exception.getMessage());
    }

}
