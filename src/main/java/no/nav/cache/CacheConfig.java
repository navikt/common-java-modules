package no.nav.cache;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Wither
@Builder
public class CacheConfig {

    public static final CacheConfig DEFAULT = CacheConfig.builder().build();
    private static final long FIVE_MINUTES = 5 * 60 * 1000;

    @Builder.Default
    public long timeToLive = FIVE_MINUTES;
    @Builder.Default
    public int maxEntries = 1000;

}
