package no.nav.cache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Function;

import static net.sf.ehcache.store.MemoryStoreEvictionPolicy.LRU;

public class CacheUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheUtils.class);
    private static final CacheManager CACHE_MANAGER = CacheManager.newInstance(new Configuration());

    public static <T> Cache<String, T> buildCache(CacheConfig cacheConfig) {
        return new CacheImpl(createEhCache(cacheConfig), cacheConfig);
    }

    private static net.sf.ehcache.Cache createEhCache(CacheConfig cacheConfig) {
        long timeToLiveSeconds = cacheConfig.timeToLiveMillis / 1000;

        CacheConfiguration cacheConfiguration = new CacheConfiguration(UUID.randomUUID().toString(), cacheConfig.maxEntries)
                .memoryStoreEvictionPolicy(LRU)
                .timeToIdleSeconds(timeToLiveSeconds)
                .timeToLiveSeconds(timeToLiveSeconds);

        CACHE_MANAGER.addCache(new net.sf.ehcache.Cache(cacheConfiguration));
        return CACHE_MANAGER.getCache(cacheConfiguration.getName());
    }

    private static class CacheImpl<T> implements Cache<String, T> {
        private final net.sf.ehcache.Cache cache;
        private final long refreshInterval;

        public CacheImpl(net.sf.ehcache.Cache cache, CacheConfig cacheConfig) {
            this.refreshInterval = cacheConfig.timeToLiveMillis / 2;
            this.cache = cache;
        }

        @Override
        public T get(String key, Function<String, T> function) {
            Element element = cache.get(key);
            if (element == null) {
                element = update(key, function);
            } else if (shouldRefresh(element)) {
                try {
                    element = update(key, function);
                } catch (Throwable t) {
                    LOGGER.warn("failed to refresh cache", t);
                }
            }
            return (T) element.getObjectValue();
        }

        private boolean shouldRefresh(Element element) {
            return element.getExpirationTime() - refreshInterval < System.currentTimeMillis();
        }

        private Element update(String key, Function<String, T> function) {
            Element element;
            element = new Element(key, function.apply(key));
            cache.put(element);
            return element;
        }
    }
}

