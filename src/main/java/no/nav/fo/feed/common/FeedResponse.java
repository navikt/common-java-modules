package no.nav.fo.feed.common;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class FeedResponse<ID extends Comparable<ID>, DOMAINOBJECT> {
    ID nextPageId;
    List<FeedElement<ID, DOMAINOBJECT>> elements;
}
