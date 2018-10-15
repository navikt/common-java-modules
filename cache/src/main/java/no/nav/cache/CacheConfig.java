package no.nav.cache;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.concurrent.TimeUnit;

@Value
@Wither
@Builder
public class CacheConfig {

    public static final CacheConfig DEFAULT = CacheConfig.builder().build();
    private static final long FIVE_MINUTES = 5 * 60 * 1000;

    @Builder.Default
    public long timeToLiveMillis = FIVE_MINUTES;
    @Builder.Default
    public int maxEntries = 1000;

    public static class CacheConfigBuilder {
        public CacheConfigBuilder timeToLive(long millis) {
            return timeToLiveMillis(millis);
        }
        public CacheConfigBuilder timeToLive(long duration, TimeUnit timeUnit) {
            return timeToLiveMillis(timeUnit.toMillis(duration));
        }
    }

}
