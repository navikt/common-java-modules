package no.nav.common.feed.common;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
@EqualsAndHashCode
public class FeedResponse<DOMAINOBJECT extends Comparable<DOMAINOBJECT>> {
    String nextPageId;
    List<FeedElement<DOMAINOBJECT>> elements;
}
