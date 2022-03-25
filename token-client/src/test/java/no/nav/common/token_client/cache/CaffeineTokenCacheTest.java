package no.nav.common.token_client.cache;

import no.nav.common.token_client.test_utils.TokenCreator;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CaffeineTokenCacheTest {

    @Test
    public void should_cache_with_same_key() {
        CaffeineTokenCache tokenCache = new CaffeineTokenCache();

        AtomicInteger counter = new AtomicInteger();

        Supplier<String> supplier = () -> {
            counter.incrementAndGet();
            return TokenCreator.instance().createToken();
        };

        tokenCache.getFromCacheOrTryProvider("key1", supplier);
        tokenCache.getFromCacheOrTryProvider("key1", supplier);

        assertEquals(1, counter.get());
    }

    @Test
    public void should_not_cache_different_keys() {
        CaffeineTokenCache tokenCache = new CaffeineTokenCache();

        AtomicInteger counter = new AtomicInteger();

        Supplier<String> supplier = () -> {
            counter.incrementAndGet();
            return TokenCreator.instance().createToken();
        };

        tokenCache.getFromCacheOrTryProvider("key1", supplier);
        tokenCache.getFromCacheOrTryProvider("key1", supplier);
        tokenCache.getFromCacheOrTryProvider("key2", supplier);

        assertEquals(2, counter.get());
    }

}
