package no.nav.fo.feed.producer;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class FeedResponse<T> {
    ZonedDateTime nextPageId;
    List<FeedElement<T>> elements;
}
