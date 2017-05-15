package no.nav.fo.feed.producer;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.time.LocalDateTime;

public class FeedRequest {

    @QueryParam("since_id")
    LocalDateTime sinceId;

    @QueryParam("page_size")
    @DefaultValue("100")
    int pageSize;
}

