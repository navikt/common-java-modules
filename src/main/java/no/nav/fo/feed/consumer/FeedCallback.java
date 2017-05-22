package no.nav.fo.feed.consumer;

import java.util.List;

@FunctionalInterface
public interface FeedCallback<T> {
    void call(List<T> data);
}
