package no.nav.fo.feed.controller;

import no.nav.fo.feed.common.FeedRequest;
import no.nav.fo.feed.common.FeedWebhookRequest;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.producer.FeedProducer;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Consumes("application/json")
@Produces("application/json")
@Path("feed")
public class FeedController {

    private Map<String, FeedProducer> producers = new HashMap<>();
    private Map<String, FeedConsumer> consumers = new HashMap<>();

    public <ID extends Comparable<ID>, DOMAINOBJECT> void addFeed(String serverFeedname, FeedProducer<ID, DOMAINOBJECT> producer) {
        producers.put(serverFeedname, producer);
    }

    public <ID extends Comparable<ID>, DOMAINOBJECT> void addFeed(String clientFeedname, FeedConsumer<ID, DOMAINOBJECT> consumer) {
        consumers.put(clientFeedname, consumer);
    }

    // PRODUCER CONTROLLER

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
    public Response get(@PathParam("name") String name, @BeanParam FeedRequest request) {
        return Optional.ofNullable(producers.get(name))
                .map((feed) -> feed.getFeedPage(request))
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
