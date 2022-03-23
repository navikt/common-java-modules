package no.nav.common.token_client.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nimbusds.jwt.JWT;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static no.nav.common.token_client.utils.TokenUtils.expiresWithin;
import static no.nav.common.token_client.utils.TokenUtils.parseJwtToken;

public class TokenCache {

    // TODO: TokenX has max lifetime of 5 min

    private final static long DEFAULT_EXPIRE_BEFORE_REFRESH_MS = 60 * 1000; // 1 minute

    private final Map<String, JWT> cachedTokens = new ConcurrentHashMap<>();

    private final Cache<String, JWT> tokenCache;

    private final long expireBeforeRefreshMs;

    public TokenCache(long expireBeforeRefreshMs) {
        this.expireBeforeRefreshMs = expireBeforeRefreshMs;
        tokenCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(100_000)
                .build();
    }

    public TokenCache() {
        this(DEFAULT_EXPIRE_BEFORE_REFRESH_MS);
    }

    public String getFromCacheOrTryProvider(String cacheKey, Supplier<String> tokenProvider) {
        JWT token = cachedTokens.get(cacheKey);

        if (expiresWithin(token, expireBeforeRefreshMs)) {
            token = parseJwtToken(tokenProvider.get());
            cachedTokens.put(cacheKey, token);
        }

        return token.getParsedString();
    }

}
