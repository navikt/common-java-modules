package no.nav.common.token_client_cache_valkey;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import io.lettuce.core.RedisException;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisCommands;
import no.nav.common.token_client.cache.TokenCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static no.nav.common.token_client.utils.TokenUtils.getCachedTokenTtl;

public class ValkeyTokenCache implements TokenCache {

    private static final Logger LOG = LoggerFactory.getLogger(ValkeyTokenCache.class);

    private static final Duration DEFAULT_SAFETY_MARGIN = Duration.ofSeconds(30);

    private static final Duration ERROR_LOG_INTERVAL = Duration.ofMinutes(1);

    private final RedisCommands<String, String> commands;

    private final String namespace;

    private final Duration safetyMargin;

    private final Clock clock;

    private final AtomicLong nextReadErrorLogAt = new AtomicLong(Long.MIN_VALUE);

    private final AtomicLong nextWriteErrorLogAt = new AtomicLong(Long.MIN_VALUE);

    public ValkeyTokenCache(RedisCommands<String, String> commands, String namespace) {
        this(commands, namespace, DEFAULT_SAFETY_MARGIN, Clock.systemUTC());
    }

    public ValkeyTokenCache(RedisCommands<String, String> commands, String namespace,
                            Duration safetyMargin, Clock clock) {
        this.commands = Objects.requireNonNull(commands, "Valkey commands are required");
        validateNamespace(namespace);
        this.namespace = namespace;
        this.safetyMargin = Objects.requireNonNull(safetyMargin, "Safety margin is required");
        if (safetyMargin.isNegative()) {
            throw new IllegalArgumentException("Safety margin must not be negative");
        }
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    @Override
    public String getFromCacheOrTryProvider(String cacheKey, Supplier<String> tokenProvider) {
        Objects.requireNonNull(tokenProvider, "Token provider is required");
        String key = createNamespacedCacheKey(namespace, cacheKey);
        String cachedToken = readCachedToken(key);
        if (cachedToken != null) {
            return cachedToken;
        }

        String token = Objects.requireNonNull(tokenProvider.get(), "Token provider returned null");
        JWT jwt = parseToken(token);
        Instant now = clock.instant();
        Clock snapshot = Clock.fixed(now, clock.getZone());
        if (getCachedTokenTtl(jwt, Duration.ZERO, snapshot).isZero()) {
            throw new IllegalArgumentException("Token provider returned an expired token");
        }

        Duration ttl = getCachedTokenTtl(jwt, safetyMargin, snapshot);
        if (!ttl.isZero()) {
            writeToken(key, token, now.plus(ttl));
        }
        return token;
    }

    private String readCachedToken(String key) {
        try {
            String token = commands.get(key);
            if (token == null || getCachedTokenTtl(parseToken(token), safetyMargin, clock).isZero()) {
                return null;
            }
            return token;
        } catch (RedisException | IllegalArgumentException e) {
            logReadError();
            return null;
        }
    }

    private void writeToken(String key, String token, Instant expiresAt) {
        try {
            commands.set(key, token, SetArgs.Builder.pxAt(expiresAt));
        } catch (RedisException e) {
            logWriteError();
        }
    }

    private void logReadError() {
        if (shouldLog(nextReadErrorLogAt)) {
            LOG.warn("Valkey token cache read failed; treating the result as a cache miss");
        }
    }

    private void logWriteError() {
        if (shouldLog(nextWriteErrorLogAt)) {
            LOG.warn("Valkey token cache write failed; returning the exchanged token without caching it");
        }
    }

    private boolean shouldLog(AtomicLong nextLogAt) {
        long now = clock.millis();
        long next = nextLogAt.get();
        return now >= next && nextLogAt.compareAndSet(next, now + ERROR_LOG_INTERVAL.toMillis());
    }

    private static JWT parseToken(String token) {
        try {
            return JWTParser.parse(token);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Cached or provided token could not be parsed");
        }
    }

    static String createNamespacedCacheKey(String namespace, String cacheKey) {
        requireValue(namespace, "Namespace");
        requireValue(cacheKey, "Cache key");

        MessageDigest digest = sha256();
        updateField(digest, namespace);
        updateField(digest, cacheKey);
        return "token-cache:v1:" + HexFormat.of().formatHex(digest.digest());
    }

    static void validateNamespace(String namespace) {
        Objects.requireNonNull(namespace, "Namespace is required");
        if (namespace.isBlank()) {
            throw new IllegalArgumentException("Namespace must not be blank");
        }
    }

    private static void updateField(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for token cache keys", e);
        }
    }

    private static void requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
