package no.nav.common.feed.common;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeedWebhookRequest {
    public String callbackUrl;
}