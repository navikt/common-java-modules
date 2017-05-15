package no.nav.fo.feed.producer;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeedElement<E, T> {
    protected T id;
    protected E element;
}
