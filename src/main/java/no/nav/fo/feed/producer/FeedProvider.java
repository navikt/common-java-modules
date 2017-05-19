package no.nav.fo.feed.producer;

import no.nav.fo.feed.common.FeedElement;

import java.util.stream.Stream;

@FunctionalInterface
public interface FeedProvider<ID extends Comparable<ID>, DOMAINOBJECT> {
    Stream<FeedElement<ID, DOMAINOBJECT>> fetchData(ID sinceId, int pageSize);
}
