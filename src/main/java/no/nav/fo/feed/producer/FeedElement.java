package no.nav.fo.feed.producer;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class FeedElement<T> implements Comparable<FeedElement<T>>{
    protected ZonedDateTime id;
    protected T element;

    @Override
    public int compareTo(FeedElement<T> o) {
        return id.compareTo(o.getId());
    }
}
