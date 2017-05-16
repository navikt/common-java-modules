package no.nav.fo.feed.producer;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class FeedElement<T> {
    protected ZonedDateTime id;
    protected T element;
}
