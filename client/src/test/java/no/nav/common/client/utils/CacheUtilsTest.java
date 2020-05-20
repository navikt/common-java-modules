package no.nav.common.client.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.Test;

import java.util.function.Supplier;

import static org.mockito.Mockito.*;

public class CacheUtilsTest {

    @Test
    public void skal_cache_for_samme_key() {
        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(5)
                .build();

        // Cannot spy on lambdas
        Supplier<String> supplier = spy(new Supplier<String>() {
            @Override
            public String get() {
                return "value";
            }
        });

        CacheUtils.tryCacheFirst(cache, "key1", supplier);
        CacheUtils.tryCacheFirst(cache, "key1", supplier);

        verify(supplier, times(1)).get();
    }

    @Test
    public void skal_ikke_cache_for_forskjellig_keys() {
        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(5)
                .build();

        // Cannot spy on lambdas
        Supplier<String> supplier = spy(new Supplier<String>() {
            @Override
            public String get() {
                return "value";
            }
        });

        CacheUtils.tryCacheFirst(cache, "key1", supplier);
        CacheUtils.tryCacheFirst(cache, "key2", supplier);

        verify(supplier, times(2)).get();
    }

}
