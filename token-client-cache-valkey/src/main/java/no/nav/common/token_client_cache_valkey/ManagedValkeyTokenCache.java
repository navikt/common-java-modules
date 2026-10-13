package no.nav.common.token_client_cache_valkey;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslVerifyMode;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import no.nav.common.token_client.cache.TokenCache;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class ManagedValkeyTokenCache implements TokenCache, AutoCloseable {

    private static final int REQUEST_QUEUE_SIZE = 100;

    private final RedisClient client;

    private final StatefulRedisConnection<String, String> connection;

    private final ValkeyTokenCache delegate;

    private final AtomicBoolean closed = new AtomicBoolean();

    ManagedValkeyTokenCache(RedisClient client, StatefulRedisConnection<String, String> connection,
                            ValkeyTokenCache delegate) {
        this.client = client;
        this.connection = connection;
        this.delegate = delegate;
    }

    public static ManagedValkeyTokenCache connect(RedisURI redisUri, String namespace, Duration commandTimeout) {
        validateConnectionConfiguration(redisUri, commandTimeout);
        ValkeyTokenCache.validateNamespace(namespace);

        RedisURI configuredUri = RedisURI.builder(redisUri).withTimeout(commandTimeout).build();
        RedisClient client = RedisClient.create(configuredUri);
        client.setOptions(clientOptions(commandTimeout));
        try {
            StatefulRedisConnection<String, String> connection = client.connect();
            connection.setTimeout(commandTimeout);
            return new ManagedValkeyTokenCache(
                    client, connection, new ValkeyTokenCache(connection.sync(), namespace));
        } catch (RuntimeException e) {
            shutdownPreserving(client, e);
            throw e;
        }
    }

    @Override
    public String getFromCacheOrTryProvider(String cacheKey, Supplier<String> tokenProvider) {
        return delegate.getFromCacheOrTryProvider(cacheKey, tokenProvider);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        RuntimeException closeFailure = null;
        try {
            connection.close();
        } catch (RuntimeException e) {
            closeFailure = e;
        } finally {
            try {
                client.shutdown();
            } catch (RuntimeException e) {
                if (closeFailure != null) {
                    closeFailure.addSuppressed(e);
                } else {
                    throw e;
                }
            }
        }
        if (closeFailure != null) {
            throw closeFailure;
        }
    }

    static ClientOptions clientOptions(Duration commandTimeout) {
        return ClientOptions.builder()
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .replayFilter(command -> true)
                .requestQueueSize(REQUEST_QUEUE_SIZE)
                .timeoutOptions(TimeoutOptions.enabled(commandTimeout))
                .build();
    }

    static void validateConnectionConfiguration(RedisURI redisUri, Duration commandTimeout) {
        Objects.requireNonNull(redisUri, "Valkey URI is required");
        Objects.requireNonNull(commandTimeout, "Command timeout is required");
        if (!redisUri.isSsl() || redisUri.getVerifyMode() != SslVerifyMode.FULL) {
            throw new IllegalArgumentException("Valkey connection must use TLS with full certificate verification");
        }
        if (commandTimeout.isZero() || commandTimeout.isNegative()) {
            throw new IllegalArgumentException("Command timeout must be positive");
        }
    }

    private static void shutdownPreserving(RedisClient client, RuntimeException originalFailure) {
        try {
            client.shutdown();
        } catch (RuntimeException shutdownFailure) {
            originalFailure.addSuppressed(shutdownFailure);
        }
    }
}
