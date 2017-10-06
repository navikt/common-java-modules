package no.nav.sbl.dialogarena.common.abac.pep.context;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static net.sf.ehcache.config.PersistenceConfiguration.Strategy.LOCALTEMPSWAP;
import static net.sf.ehcache.store.MemoryStoreEvictionPolicy.LRU;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String ASK_FOR_PERMISSION = "askForPermission";

    @Bean
    public net.sf.ehcache.CacheManager ehCacheManager() {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        config.addCache(setupCache(ASK_FOR_PERMISSION));
        return net.sf.ehcache.CacheManager.newInstance(config);
    }

    @Override
    public CacheManager cacheManager() {
        return new EhCacheCacheManager(ehCacheManager());
    }

    @Override
    public CacheResolver cacheResolver() {
        return null;
    }

    @Override
    public KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return null;
    }

    private CacheConfiguration setupCache(String name) {
        return new CacheConfiguration(name, 10000)
                .memoryStoreEvictionPolicy(LRU)
                .timeToIdleSeconds(3600)
                .timeToLiveSeconds(3600)
                .persistence(new PersistenceConfiguration().strategy(LOCALTEMPSWAP));
    }
}