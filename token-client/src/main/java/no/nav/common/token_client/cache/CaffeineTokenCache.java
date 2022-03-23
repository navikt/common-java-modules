package no.nav.common.token_client.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nimbusds.jwt.JWT;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static no.nav.common.token_client.utils.TokenUtils.expiresWithin;
import static no.nav.common.token_client.utils.TokenUtils.parseJwtToken;

public class CaffeineTokenCache implements TokenCache {

    private final static long DEFAULT_EXPIRE_BEFORE_REFRESH_MS = 30 * 1000; // 30 seconds

    private final static long DEFAULT_EXPIRE_AFTER_WRITE_MS = 60 * 60 * 1000; // 1 hour

    private final long expireBeforeRefreshMs;

    private final Cache<String, JWT> tokenCache;

    public CaffeineTokenCache() {
        this.expireBeforeRefreshMs = DEFAULT_EXPIRE_BEFORE_REFRESH_MS;
        tokenCache = Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_EXPIRE_AFTER_WRITE_MS, TimeUnit.MILLISECONDS)
                .build();
    }

    public CaffeineTokenCache(long expireBeforeRefreshMs, Cache<String, JWT> tokenCache) {
        this.expireBeforeRefreshMs = expireBeforeRefreshMs;
        this.tokenCache = tokenCache;
    }

    @Override
    public String getFromCacheOrTryProvider(String cacheKey, Supplier<String> tokenProvider) {
        JWT token = tokenCache.getIfPresent(cacheKey);

        if (expiresWithin(token, expireBeforeRefreshMs)) {
            token = parseJwtToken(tokenProvider.get());
            tokenCache.put(cacheKey, token);
        }

        return token.getParsedString();
    }

}
