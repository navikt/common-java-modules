package no.nav.common.client.utils;

import com.github.benmanes.caffeine.cache.Cache;

import java.util.function.Supplier;

public class CacheUtils {

    public static <K, V> V tryCacheFirst(Cache<K, V> cache, K key, Supplier<V> valueSupplier) {
        V value = cache.getIfPresent(key);

        if (value == null) {
            V newValue = valueSupplier.get();

            if (newValue == null) {
                return null;
            }

            cache.put(key, newValue);

            return newValue;
        }

        return value;
    }

}
