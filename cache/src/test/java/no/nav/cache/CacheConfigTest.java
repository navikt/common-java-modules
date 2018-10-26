package no.nav.cache;

import org.junit.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;


public class CacheConfigTest {

    @Test
    public void customBuilder() {
        CacheConfig cacheConfig = CacheConfig.builder()
                .maxEntries(42)
                .timeToLive(33, SECONDS)
                .build();

        assertThat(cacheConfig.maxEntries).isEqualTo(42);
        assertThat(cacheConfig.timeToLiveMillis).isEqualTo(33_000L);
    }

}