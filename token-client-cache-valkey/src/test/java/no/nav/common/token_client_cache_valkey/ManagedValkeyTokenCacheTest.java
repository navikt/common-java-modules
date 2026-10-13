package no.nav.common.token_client_cache_valkey;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslVerifyMode;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ManagedValkeyTokenCacheTest {

    @Test
    public void shouldConfigureBoundedCommandsAndRejectCommandsWhileDisconnected() {
        ClientOptions options = ManagedValkeyTokenCache.clientOptions(Duration.ofMillis(100));

        assertTrue(options.isAutoReconnect());
        assertTrue(options.isRejectCommandsWhileDisconnected());
        assertTrue(options.getReplayFilter().test(mock()));
        assertEquals(100, options.getRequestQueueSize());
        assertTrue(options.getTimeoutOptions().isTimeoutCommands());
    }

    @Test
    public void shouldRequireTlsWithFullCertificateVerification() {
        RedisURI plainText = RedisURI.Builder.redis("valkey.example").build();
        RedisURI noVerification = RedisURI.Builder.redis("valkey.example")
                .withSsl(true)
                .withVerifyPeer(SslVerifyMode.NONE)
                .build();
        RedisURI caOnly = RedisURI.Builder.redis("valkey.example")
                .withSsl(true)
                .withVerifyPeer(SslVerifyMode.CA)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> ManagedValkeyTokenCache.validateConnectionConfiguration(
                        plainText, Duration.ofMillis(100)));
        assertThrows(IllegalArgumentException.class,
                () -> ManagedValkeyTokenCache.validateConnectionConfiguration(
                        noVerification, Duration.ofMillis(100)));
        assertThrows(IllegalArgumentException.class,
                () -> ManagedValkeyTokenCache.validateConnectionConfiguration(
                        caOnly, Duration.ofMillis(100)));
    }

    @Test
    public void shouldRequirePositiveTimeout() {
        RedisURI secureUri = secureUri();

        assertThrows(IllegalArgumentException.class,
                () -> ManagedValkeyTokenCache.validateConnectionConfiguration(secureUri, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> ManagedValkeyTokenCache.validateConnectionConfiguration(
                        secureUri, Duration.ofMillis(-1)));
    }

    @Test
    public void shouldRejectBlankNamespaceBeforeConnecting() {
        assertThrows(IllegalArgumentException.class,
                () -> ManagedValkeyTokenCache.connect(secureUri(), " ", Duration.ofMillis(100)));
    }

    @Test
    public void shouldDelegateCacheOperations() {
        RedisClient client = mock();
        StatefulRedisConnection<String, String> connection = mock();
        ValkeyTokenCache delegate = mock();
        ManagedValkeyTokenCache cache = new ManagedValkeyTokenCache(client, connection, delegate);
        Supplier<String> provider = () -> "provided-token";
        when(delegate.getFromCacheOrTryProvider("key", provider)).thenReturn("cached-token");

        assertEquals("cached-token", cache.getFromCacheOrTryProvider("key", provider));
    }

    @Test
    public void shouldCloseConnectionAndClientOnlyOnce() {
        RedisClient client = mock();
        StatefulRedisConnection<String, String> connection = mock();
        ManagedValkeyTokenCache cache = new ManagedValkeyTokenCache(client, connection, mock());

        cache.close();
        cache.close();

        verify(connection).close();
        verify(client).shutdown();
    }

    @Test
    public void shouldShutdownClientWhenClosingConnectionFails() {
        RedisClient client = mock();
        StatefulRedisConnection<String, String> connection = mock();
        RuntimeException failure = new IllegalStateException("Close failed");
        doThrow(failure).when(connection).close();
        ManagedValkeyTokenCache cache = new ManagedValkeyTokenCache(client, connection, mock());

        assertSame(failure, assertThrows(IllegalStateException.class, cache::close));
        verify(client).shutdown();
    }

    @Test
    public void shouldPreserveConnectionFailureWhenShutdownAlsoFails() {
        RedisClient client = mock();
        StatefulRedisConnection<String, String> connection = mock();
        RuntimeException closeFailure = new IllegalStateException("Close failed");
        RuntimeException shutdownFailure = new IllegalStateException("Shutdown failed");
        doThrow(closeFailure).when(connection).close();
        doThrow(shutdownFailure).when(client).shutdown();
        ManagedValkeyTokenCache cache = new ManagedValkeyTokenCache(client, connection, mock());

        RuntimeException thrown = assertThrows(IllegalStateException.class, cache::close);
        assertSame(closeFailure, thrown);
        assertArrayEquals(new Throwable[]{shutdownFailure}, thrown.getSuppressed());
    }

    private RedisURI secureUri() {
        return RedisURI.Builder.redis("valkey.example")
                .withSsl(true)
                .withVerifyPeer(SslVerifyMode.FULL)
                .build();
    }
}
