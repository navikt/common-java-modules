package no.nav.fo.feed.consumer;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.common.FeedParameterizedType;
import no.nav.fo.feed.common.FeedResponse;
import no.nav.fo.feed.common.FeedWebhookRequest;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static no.nav.fo.feed.consumer.FeedPoller.createScheduledJob;
import static no.nav.fo.feed.util.UrlUtils.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;

public class FeedConsumer<DOMAINOBJECT extends Comparable<DOMAINOBJECT>> {
    public static String applicationApiroot;
    private static final Logger LOG = getLogger(FeedConsumer.class);

    private FeedConsumerConfig<DOMAINOBJECT> config;

    public FeedConsumer(FeedConsumerConfig<DOMAINOBJECT> config) {
        this.config = config;

        createScheduledJob(this.config.feedName, this.config.host, this.config.pollingInterval, this::poll);
        createScheduledJob(this.config.feedName + "/webhook", this.config.host, this.config.webhookPollingInterval, this::registerWebhook);
    }

    public boolean webhookCallback() {
        if (isBlank(this.config.webhookPollingInterval)) {
            return false;
        }

        poll();
        return true;
    }

    public void addCallback(FeedCallback callback) {
        this.config.callback(callback);
    }

    void registerWebhook() {
        Client client = ClientBuilder.newBuilder().build();

        String callbackUrl = callbackUrl(FeedConsumer.applicationApiroot, this.config.feedName);
        FeedWebhookRequest body = new FeedWebhookRequest().setCallbackUrl(callbackUrl);

        Entity<FeedWebhookRequest> entity = Entity.entity(body, APPLICATION_JSON_TYPE);

        Invocation.Builder request = client
                .target(asUrl(this.config.host, "feed", this.config.feedName, "webhook"))
                .request();

        config.interceptors.forEach( interceptor -> interceptor.apply(request));

        Response response = request
                .buildPut(entity)
                .invoke();

        if (response.getStatus() == 201) {
            LOG.info("Webhook opprettet hos produsent!");
        } else if (response.getStatus() != 200) {
            LOG.warn("Endepunkt for opprettelse av webhook returnerte feilkode {}", response.getStatus());
        }
    }

    void poll() {
        String lastEntry = this.config.lastEntrySupplier.get();
        Invocation.Builder request = ClientBuilder.newBuilder().build()
                .target(asUrl(this.config.host, "feed", this.config.feedName))
                .queryParam(QUERY_PARAM_ID, lastEntry)
                .queryParam(QUERY_PARAM_PAGE_SIZE, this.config.pageSize)
                .request();

        config.interceptors.forEach( interceptor -> interceptor.apply(request));

        Response response = request
                .buildGet()
                .invoke();

        if (response.getStatus() != 200) {
            LOG.warn("Endepunkt for polling av feed returnerte feilkode {}", response.getStatus());
        }

        ParameterizedType type = new FeedParameterizedType(this.config.domainobject);
        FeedResponse<DOMAINOBJECT> entity = response.readEntity(new GenericType<>(type));

        if (entity.getElements() != null && !entity.getElements().isEmpty()) {
            List<DOMAINOBJECT> data = entity
                    .getElements()
                    .stream()
                    .map(FeedElement::getElement)
                    .collect(Collectors.toList());

            this.config.callback.call(entity.getNextPageId(), data);
        }
    }
}
