package no.nav.fo.feed.producer;

import java.time.LocalDateTime;
import java.util.stream.Stream;

public interface FeedProvider<T> {
    Stream<FeedElement<T>> fetchData(LocalDateTime sinceId, int pageSize);
}
