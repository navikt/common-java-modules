package no.nav.fo.feed.consumer;

import lombok.experimental.Builder;
import no.nav.fo.feed.producer.FeedWebhookRequest;
import no.nav.metrics.aspects.Timed;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

@Builder
public class FeedConsumer<T> {
    private static final Logger LOG = getLogger(FeedConsumer.class);

    private String feedProducerUrl;
    private String feedProducerUrlWebhook;
    private FeedCallback onWebhook;

    public void callback() {
        onWebhook.callback();
    }

    @Timed(name = "feed.registerWebhook")
    public void registerWebhook(String callbackUrl) {

        Client client = ClientBuilder.newBuilder().build();
        FeedWebhookRequest body = new FeedWebhookRequest().setCallbackUrl(callbackUrl);
        Entity<FeedWebhookRequest> entity = Entity.entity(body, APPLICATION_JSON_TYPE);
        Response response = client
                .target(feedProducerUrlWebhook)
                .request()
                .buildPut(entity)
                .invoke();

        if (response.getStatus() == 201) {
            LOG.info("Webhook opprettet hos produsent!");
        }
        else if (response.getStatus() != 200) {
            LOG.warn("Endepunkt for opprettelse av webhook returnerte feilkode {}", response.getStatus());
        }
    }

    @Timed(name = "feed.poll")
    public Response poll(T sinceId, int pageSize) {

        Client client = ClientBuilder.newBuilder().build();
        Response response = client
                .target(feedProducerUrl)
                .queryParam("since_id", sinceId)
                .request()
                .buildGet()
                .invoke();


        if (response.getStatus() != 200) {
            LOG.warn("Endepunkt for polling av feed returnerte feilkode {}", response.getStatus());
        }
        return response;
    }
}
