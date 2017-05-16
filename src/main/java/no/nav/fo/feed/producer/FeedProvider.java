package no.nav.fo.feed.producer;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

public interface FeedProvider<T> {
    Stream<FeedElement<T>> fetchData(ZonedDateTime sinceId, int pageSize);
}
