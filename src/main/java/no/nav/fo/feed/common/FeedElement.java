package no.nav.fo.feed.common;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeedElement<DOMAINOBJECT> implements Comparable<FeedElement<DOMAINOBJECT>>{
    protected String id;
    protected DOMAINOBJECT element;

    @Override
    public int compareTo(FeedElement<DOMAINOBJECT> o) {
        return id.compareTo(o.getId());
    }
}
