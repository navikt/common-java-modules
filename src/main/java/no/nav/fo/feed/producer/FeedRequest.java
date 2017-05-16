package no.nav.fo.feed.producer;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.time.ZonedDateTime;

public class FeedRequest {

    @QueryParam("since_id")
    ZonedDateTime sinceId;

    @QueryParam("page_size")
    @DefaultValue("100")
    int pageSize;
}

