package no.nav.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CacheUtilsTest {

    public static final int TIME_TO_LIVE = 2000;
    private Function<String, String> cachedFunction = mock(Function.class);
    private Cache<String, String> stringCache = CacheUtils.buildCache(CacheConfig.builder().timeToLive(TIME_TO_LIVE).build());

    @BeforeEach
    public void setup() {
        when(cachedFunction.apply(any())).then((i) -> i.getArgument(0).toString().toUpperCase());
    }

    @Test
    public void api() {
        Cache<String, Integer> integerCache = defaultCache();
        assertThat(integerCache.get("1", Integer::parseInt)).isEqualTo(1);

        Cache<String, String> staticCache = defaultCache();
        assertThat(staticCache.get("a", () -> "STATIC VALUE")).isEqualTo("STATIC VALUE");

        Cache<String, String> dynamicCache = defaultCache();
        assertThat(dynamicCache.get("a", String::toUpperCase)).isEqualTo("A");
        assertThat(dynamicCache.get("b", String::toUpperCase)).isEqualTo("B");
    }

    @Test
    public void buildCache() {
        assertThat(stringCache.get("a", cachedFunction)).isEqualTo("A"); // fetch
        assertThat(stringCache.get("a", cachedFunction)).isEqualTo("A"); // hit
        assertThat(stringCache.get("a", cachedFunction)).isEqualTo("A"); // hit

        assertThat(stringCache.get("b", cachedFunction)).isEqualTo("B"); // fetch
        assertThat(stringCache.get("b", cachedFunction)).isEqualTo("B"); // hit
        assertThat(stringCache.get("b", cachedFunction)).isEqualTo("B"); // hit

        verify(cachedFunction, times(1)).apply("a");
        verify(cachedFunction, times(1)).apply("b");
    }

    @Test
    public void buildCache__expiration() throws InterruptedException {
        assertThat(stringCache.get("a", cachedFunction)).isEqualTo("A"); // fetch
        assertThat(stringCache.get("a", cachedFunction)).isEqualTo("A"); // hit

        Thread.sleep(TIME_TO_LIVE + 1);

        assertThat(stringCache.get("a", cachedFunction)).isEqualTo("A"); // fetch
        assertThat(stringCache.get("a", cachedFunction)).isEqualTo("A"); // hit

        verify(cachedFunction, times(2)).apply("a");
    }

    @Test
    public void buildCache__refresh_before_expiration() throws InterruptedException {
        assertThat(stringCache.get("a", cachedFunction)).isEqualTo("A"); // fetch

        Function<String, String> failingFunction = mock(Function.class);
        when(failingFunction.apply(any())).thenThrow(TestException.class);
        assertThat(stringCache.get("a", failingFunction)).isEqualTo("A"); // hit
        assertThat(stringCache.get("a", failingFunction)).isEqualTo("A"); // hit

        Thread.sleep(TIME_TO_LIVE / 2 + 1);

        assertThat(stringCache.get("a", failingFunction)).isEqualTo("A"); // refresh
        assertThat(stringCache.get("a", failingFunction)).isEqualTo("A"); // refresh

        Thread.sleep(TIME_TO_LIVE);
        assertThatThrownBy(() -> stringCache.get("a", failingFunction)).isInstanceOf(TestException.class);

        verify(failingFunction, times(3)).apply("a");
    }

    private <T> Cache<String, T> defaultCache() {
        return CacheUtils.buildCache(CacheConfig.DEFAULT);
    }

    private static class TestException extends RuntimeException {
    }
}