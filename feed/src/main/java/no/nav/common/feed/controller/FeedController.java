package no.nav.common.feed.controller;

import no.nav.common.feed.common.Authorization;
import no.nav.common.feed.common.FeedRequest;
import no.nav.common.feed.common.FeedResponse;
import no.nav.common.feed.common.FeedWebhookRequest;
import no.nav.common.feed.consumer.FeedConsumer;
import no.nav.common.feed.exception.MissingIdException;
import no.nav.common.feed.producer.FeedProducer;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static no.nav.common.feed.util.UrlUtils.QUERY_PARAM_ID;
import static no.nav.common.feed.util.UrlUtils.QUERY_PARAM_PAGE_SIZE;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Consumes("application/json")
@Produces("application/json")
@Path("feed")
public class FeedController {

    private static final Logger LOG = getLogger(FeedController.class);
    private static final int DEFAULT_PAGE_SIZE = 100;

    private Map<String, FeedProducer> producers = new HashMap<>();
    private Map<String, FeedConsumer> consumers = new HashMap<>();

    public <DOMAINOBJECT extends Comparable<DOMAINOBJECT>> FeedController addFeed(String serverFeedname, FeedProducer<DOMAINOBJECT> producer) {
        LOG.info("ny feed. navn={}", serverFeedname);
        producers.put(serverFeedname, producer);
        return this;
    }

    public <DOMAINOBJECT extends Comparable<DOMAINOBJECT>> FeedController addFeed(String clientFeedname, FeedConsumer<DOMAINOBJECT> consumer) {
        LOG.info("ny feed-klient. navn={}", clientFeedname);
        consumers.put(clientFeedname, consumer);
        return this;
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
    public Response registerWebhook(FeedWebhookRequest request, @PathParam("name") String name) {
        return ofNullable(producers.get(name))
                .map((producer) -> authorizeRequest(producer, name))
                .map((feed) -> feed.createWebhook(request))
                .map((created) -> Response.status(created ? 201 : 200))
                .orElse(Response.status(Response.Status.BAD_REQUEST)).build();
    }

    @GET
    @Path("{name}")
    public FeedResponse<?> getFeeddata(@PathParam("name") String name, @QueryParam(QUERY_PARAM_ID) String id, @QueryParam(QUERY_PARAM_PAGE_SIZE) Integer pageSize) {
        FeedProducer feedProducer = ofNullable(producers.get(name)).orElseThrow(NotFoundException::new);
        authorizeRequest(feedProducer, name);
        FeedRequest request = new FeedRequest()
                .setSinceId(ofNullable(id).orElseThrow(MissingIdException::new))
                .setPageSize(ofNullable(pageSize).orElse(DEFAULT_PAGE_SIZE));
        return feedProducer.getFeedPage(name, request);
    }

    @GET
    @Path("/feedname")
    public Response getFeedNames() {
        return Response.ok().entity(producers.keySet()).build();
    }

    // CONSUMER CONTROLLER

    @HEAD
    @Path("{name}")
    public Response webhookCallback(@PathParam("name") String feedname) {
        return ofNullable(feedname)
                .map((name) -> consumers.get(name))
                .map((consumer) -> authorizeRequest(consumer, feedname))
                .map(FeedConsumer::webhookCallback)
                .map((hadCallback) -> Response.status(hadCallback ? 200 : 404))
                .orElse(Response.status(404))
                .build();
    }

    private <T extends Authorization> T authorizeRequest(T feed, String name) {
        if (!feed.getAuthorizationModule().isRequestAuthorized(name)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return feed;
    }

}