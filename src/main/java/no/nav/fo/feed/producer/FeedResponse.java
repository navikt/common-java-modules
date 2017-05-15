package no.nav.fo.feed.producer;

import lombok.Value;

import java.util.List;

@Value
class FeedResponse<T> {
    T nextId;
    List<FeedElement> elements;
}
