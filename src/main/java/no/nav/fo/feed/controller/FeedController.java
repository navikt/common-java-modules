package no.nav.fo.feed.controller;

import no.nav.fo.feed.common.FeedRequest;
import no.nav.fo.feed.common.FeedWebhookRequest;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.exception.MissingIdException;
import no.nav.fo.feed.producer.FeedProducer;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@Consumes("application/json")
@Produces("application/json")
@Path("feed")
public class FeedController {

    private static final Logger LOG = getLogger(FeedController.class);

    private Map<String, FeedProducer> producers = new HashMap<>();
    private Map<String, FeedConsumer> consumers = new HashMap<>();

    public <DOMAINOBJECT extends Comparable<DOMAINOBJECT>> void addFeed(String serverFeedname, FeedProducer<DOMAINOBJECT> producer) {
        LOG.info("ny feed. navn={}", serverFeedname);
        producers.put(serverFeedname, producer);
    }

    public <DOMAINOBJECT extends Comparable<DOMAINOBJECT>> void addFeed(String clientFeedname, FeedConsumer<DOMAINOBJECT> consumer) {
        LOG.info("ny feed-klient. navn={}", clientFeedname);
        consumers.put(clientFeedname, consumer);
    }

    public FeedController() {
        LOG.info("starter");
    }
    // PRODUCER CONTROLLER

    @GET
    public List<String> getFeeds() {
        return new ArrayList<>(producers.keySet());
    }

    @PUT
    @Path("{name}/webhook")
    public Response putWebhook(FeedWebhookRequest request, @PathParam("name") String name) {
        return Optional.ofNullable(producers.get(name))
                .map((feed) -> feed.createWebhook(request))
                .map((created) -> Response.status(created ? 201 : 200))
                .orElse(Response.status(Response.Status.BAD_REQUEST)).build();
    }

    @GET
    @Path("{name}")
    public Response get(@PathParam("name") String name, @QueryParam("id") String id, @QueryParam("page_size") Integer pageSize) {
        String sinceId = Optional.ofNullable(id).orElseThrow(MissingIdException::new);
        int size = Optional.ofNullable(pageSize).orElse(100);
        return Optional.ofNullable(producers.get(name))
                .map((feed) -> feed.getFeedPage(new FeedRequest().setPageSize(size).setSinceId(sinceId)))
                .map(Response::ok)
                .orElse(Response.status(Response.Status.BAD_REQUEST))
                .build();
    }

    // CONSUMER CONTROLLER

    @HEAD
    @Path("{name}")
    public Response webhook(@PathParam("name") String feedname) {
        return Optional.ofNullable(feedname)
                .map((name) -> consumers.get(name))
                .map(FeedConsumer::webhookCallback)
                .map((hadCallback) -> Response.status(hadCallback ? 200 : 404))
                .orElse(Response.status(404))
                .build();
    }

}
