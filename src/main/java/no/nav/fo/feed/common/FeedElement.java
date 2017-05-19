package no.nav.fo.feed.common;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeedElement<ID extends Comparable<ID>, DOMAINOBJECT> implements Comparable<FeedElement<ID, DOMAINOBJECT>>{
    protected ID id;
    protected DOMAINOBJECT element;

    @Override
    public int compareTo(FeedElement<ID, DOMAINOBJECT> o) {
        return id.compareTo(o.getId());
    }
}
