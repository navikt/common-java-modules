package no.nav.fo.feed.producer;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeedWebhookResponse {
    private String melding;
    public String webhookUrl;
}
