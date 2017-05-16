package no.nav.fo.feed.producer;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class FeedElement<T> {
    protected LocalDateTime id;
    protected T element;
}
