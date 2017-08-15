package no.nav.fo.feed.common;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeedElement<DOMAINOBJECT extends Comparable<DOMAINOBJECT>> implements Comparable<FeedElement<DOMAINOBJECT>>{
    protected String id;
    protected DOMAINOBJECT element;

    @Override
    public int compareTo(FeedElement<DOMAINOBJECT> other) {
        return element.compareTo(other.getElement());
    }

}
