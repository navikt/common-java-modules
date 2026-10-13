package no.nav.common.token_client_cache_valkey;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import io.lettuce.core.RedisException;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static no.nav.common.token_client_cache_valkey.ValkeyTokenCache.createNamespacedCacheKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ValkeyTokenCacheTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T06:00:00Z"), ZoneOffset.UTC);
    private final RedisCommands<String, String> commands = mock();
    private final ValkeyTokenCache cache = new ValkeyTokenCache(commands, "app", Duration.ofSeconds(30), clock);

    @Test
    public void shouldReturnCachedTokenWithoutCallingProvider() {
        String token = tokenExpiringIn(120);
        when(commands.get(createNamespacedCacheKey("app", "key"))).thenReturn(token);
        Supplier<String> provider = mock();

        assertEquals(token, cache.getFromCacheOrTryProvider("key", provider));
        verifyNoInteractions(provider);
        verify(commands, never()).set(anyString(), anyString(), any(SetArgs.class));
    }

    @Test
    public void shouldStoreProviderTokenWithAtomicAbsoluteExpiration() {
        String token = tokenExpiringIn(120);

        assertEquals(token, cache.getFromCacheOrTryProvider("key", () -> token));
        assertStoredExpiration(token, clock.instant().plusSeconds(90));
    }

    @Test
    public void shouldRefreshExpiredCachedToken() {
        when(commands.get(createNamespacedCacheKey("app", "key"))).thenReturn(tokenExpiringIn(-1));
        String freshToken = tokenExpiringIn(120);

        assertEquals(freshToken, cache.getFromCacheOrTryProvider("key", () -> freshToken));
        assertStoredExpiration(freshToken, clock.instant().plusSeconds(90));
    }

    @Test
    public void shouldReturnButNotCacheProviderTokenWithinSafetyMargin() {
        String token = tokenExpiringIn(20);

        assertEquals(token, cache.getFromCacheOrTryProvider("key", () -> token));
        verify(commands, never()).set(anyString(), anyString(), any(SetArgs.class));
    }

    @Test
    public void shouldRejectExpiredProviderToken() {
        assertThrows(IllegalArgumentException.class,
                () -> cache.getFromCacheOrTryProvider("key", () -> tokenExpiringIn(-1)));
        verify(commands, never()).set(anyString(), anyString(), any(SetArgs.class));
    }

    @Test
    public void shouldRejectProviderTokenWithoutExpiration() {
        String token = new PlainJWT(new JWTClaimsSet.Builder().subject("test-user").build()).serialize();

        assertThrows(IllegalArgumentException.class,
                () -> cache.getFromCacheOrTryProvider("key", () -> token));
        verify(commands, never()).set(anyString(), anyString(), any(SetArgs.class));
    }

    @Test
    public void shouldShareTokensAcrossCacheInstancesWithSeparateClients() {
        Map<String, String> storage = new ConcurrentHashMap<>();
        ValkeyTokenCache firstPod = cacheWithSharedStorage("app", storage);
        ValkeyTokenCache secondPod = cacheWithSharedStorage("app", storage);
        AtomicInteger exchanges = new AtomicInteger();
        String token = tokenExpiringIn(120);
        Supplier<String> provider = () -> {
            exchanges.incrementAndGet();
            return token;
        };

        assertEquals(token, firstPod.getFromCacheOrTryProvider("key", provider));
        assertEquals(token, secondPod.getFromCacheOrTryProvider("key", provider));
        assertEquals(1, exchanges.get());
        assertEquals(1, storage.size());
    }

    @Test
    public void shouldKeepNamespacesSeparate() {
        Map<String, String> storage = new ConcurrentHashMap<>();
        ValkeyTokenCache firstApp = cacheWithSharedStorage("prod:app-1", storage);
        ValkeyTokenCache secondApp = cacheWithSharedStorage("prod:app-2", storage);
        String firstToken = tokenExpiringIn(120);
        String secondToken = tokenExpiringIn(240);

        assertEquals(firstToken, firstApp.getFromCacheOrTryProvider("key", () -> firstToken));
        assertEquals(secondToken, secondApp.getFromCacheOrTryProvider("key", () -> secondToken));
        assertEquals(firstToken, firstApp.getFromCacheOrTryProvider("key", () -> fail("Expected cache hit")));
        assertEquals(secondToken, secondApp.getFromCacheOrTryProvider("key", () -> fail("Expected cache hit")));
        assertEquals(2, storage.size());
    }

    @Test
    public void shouldKeepCacheIdentitiesSeparateWithinNamespace() {
        Map<String, String> storage = new ConcurrentHashMap<>();
        ValkeyTokenCache sharedCache = cacheWithSharedStorage("app", storage);
        String firstToken = tokenExpiringIn(120);
        String secondToken = tokenExpiringIn(240);

        assertEquals(firstToken, sharedCache.getFromCacheOrTryProvider("key-1", () -> firstToken));
        assertEquals(secondToken, sharedCache.getFromCacheOrTryProvider("key-2", () -> secondToken));
        assertEquals(firstToken, sharedCache.getFromCacheOrTryProvider("key-1", () -> fail("Expected cache hit")));
        assertEquals(secondToken, sharedCache.getFromCacheOrTryProvider("key-2", () -> fail("Expected cache hit")));
        assertEquals(2, storage.size());
    }

    @Test
    public void shouldAllowConcurrentMissesWithoutSingleFlight() throws Exception {
        Map<String, String> storage = new ConcurrentHashMap<>();
        ValkeyTokenCache firstPod = cacheWithSharedStorage("app", storage);
        ValkeyTokenCache secondPod = cacheWithSharedStorage("app", storage);
        CountDownLatch providersStarted = new CountDownLatch(2);
        AtomicInteger exchanges = new AtomicInteger();
        String token = tokenExpiringIn(120);
        Supplier<String> provider = () -> {
            exchanges.incrementAndGet();
            providersStarted.countDown();
            try {
                if (!providersStarted.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Both providers must start independently");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for providers", e);
            }
            return token;
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> firstPod.getFromCacheOrTryProvider("key", provider));
            var second = executor.submit(() -> secondPod.getFromCacheOrTryProvider("key", provider));
            assertEquals(token, first.get(10, TimeUnit.SECONDS));
            assertEquals(token, second.get(10, TimeUnit.SECONDS));
        }
        assertEquals(2, exchanges.get());
        assertEquals(1, storage.size());
    }

    @Test
    public void shouldTreatValkeyReadFailureAsCacheMiss() {
        when(commands.get(anyString())).thenThrow(new RedisException("contains-sensitive-command-details"));
        String token = tokenExpiringIn(120);

        assertEquals(token, cache.getFromCacheOrTryProvider("key", () -> token));
        verify(commands).set(anyString(), eq(token), any(SetArgs.class));
    }

    @Test
    public void shouldReturnExchangedTokenWhenValkeyWriteFails() {
        String token = tokenExpiringIn(120);
        when(commands.set(anyString(), anyString(), any(SetArgs.class)))
                .thenThrow(new RedisException("contains-sensitive-command-details"));

        assertEquals(token, cache.getFromCacheOrTryProvider("key", () -> token));
    }

    @Test
    public void shouldTreatMalformedCachedTokenAsCacheMiss() {
        when(commands.get(anyString())).thenReturn("sensitive-invalid-cached-token");
        String token = tokenExpiringIn(120);

        assertEquals(token, cache.getFromCacheOrTryProvider("key", () -> token));
        verify(commands).set(anyString(), eq(token), any(SetArgs.class));
    }

    @Test
    public void shouldPropagateProviderFailureUnchangedAfterReadFailure() {
        when(commands.get(anyString())).thenThrow(new RedisException("Valkey unavailable"));
        IllegalStateException providerFailure = new IllegalStateException("Token exchange failed");

        assertSame(providerFailure, assertThrows(IllegalStateException.class,
                () -> cache.getFromCacheOrTryProvider("key", () -> {
                    throw providerFailure;
                })));
        verify(commands, never()).set(anyString(), anyString(), any(SetArgs.class));
    }

    @Test
    public void shouldIsolateAndHideNamespaceAndCacheIdentity() {
        String key = createNamespacedCacheKey("sensitive-namespace", "sensitive-cache-key");

        assertTrue(key.matches("token-cache:v1:[0-9a-f]{64}"));
        assertNotEquals(key, createNamespacedCacheKey("other-namespace", "sensitive-cache-key"));
        assertNotEquals(key, createNamespacedCacheKey("sensitive-namespace", "other-cache-key"));
    }

    @Test
    public void shouldRejectBlankNamespaceAndNegativeSafetyMargin() {
        assertThrows(IllegalArgumentException.class,
                () -> new ValkeyTokenCache(commands, " ", Duration.ofSeconds(30), clock));
        assertThrows(IllegalArgumentException.class,
                () -> new ValkeyTokenCache(commands, "app", Duration.ofSeconds(-1), clock));
    }

    private String tokenExpiringIn(long seconds) {
        return new PlainJWT(new JWTClaimsSet.Builder()
                .subject("test-user")
                .expirationTime(Date.from(clock.instant().plusSeconds(seconds)))
                .build()).serialize();
    }

    private void assertStoredExpiration(String token, Instant expiration) {
        ArgumentCaptor<SetArgs> capturedArgs = ArgumentCaptor.forClass(SetArgs.class);
        verify(commands).set(eq(createNamespacedCacheKey("app", "key")), eq(token), capturedArgs.capture());
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        capturedArgs.getValue().build(args);
        assertEquals("PXAT " + expiration.toEpochMilli(), args.toCommandString());
    }

    private ValkeyTokenCache cacheWithSharedStorage(String namespace, Map<String, String> storage) {
        RedisCommands<String, String> client = mock();
        when(client.get(anyString())).thenAnswer(invocation -> storage.get(invocation.<String>getArgument(0)));
        when(client.set(anyString(), anyString(), any(SetArgs.class))).thenAnswer(invocation -> {
            storage.put(invocation.getArgument(0), invocation.getArgument(1));
            return "OK";
        });
        return new ValkeyTokenCache(client, namespace, Duration.ofSeconds(30), clock);
    }
}
