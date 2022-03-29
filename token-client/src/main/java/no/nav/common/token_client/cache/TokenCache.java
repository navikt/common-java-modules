package no.nav.common.token_client.cache;

import java.util.function.Supplier;

public interface TokenCache {

    /**
     * Checks the cache for stored tokens matching {@code cacheKey} and returns it if found.
     * If the cache does not contain the key, then retrieve a new token from {@code tokenProvider} and store it with {@code cacheKey}
     * @param cacheKey the key to use to search for and store the token
     * @param tokenProvider provides a token on cache misses
     * @return JWT token
     */
    String getFromCacheOrTryProvider(String cacheKey, Supplier<String> tokenProvider);

}
