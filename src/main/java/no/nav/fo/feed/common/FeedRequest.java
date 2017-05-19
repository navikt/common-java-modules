package no.nav.fo.feed.common;

import lombok.Data;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.time.ZonedDateTime;

@Data
public class FeedRequest {

    @QueryParam("since_id")
    String sinceId;

    @QueryParam("page_size")
    @DefaultValue("100")
    int pageSize;
}

