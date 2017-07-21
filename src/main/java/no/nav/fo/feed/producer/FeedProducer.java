package no.nav.fo.feed.producer;

import lombok.Builder;
import no.nav.fo.feed.common.*;
import no.nav.fo.feed.exception.InvalidUrlException;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.HttpMethod.HEAD;
import static no.nav.fo.feed.util.UrlValidator.validateUrl;
import static org.slf4j.LoggerFactory.getLogger;

@Builder
public class FeedProducer<DOMAINOBJECT extends Comparable<DOMAINOBJECT>> {

    private static final Logger LOG = getLogger(FeedProducer.class);

    @Builder.Default
    private int maxPageSize = 10000;
    @Builder.Default
    private List<String> callbackUrls = new ArrayList<>();
    @Builder.Default
    private List<OutInterceptor> interceptors = new ArrayList<>();
    private FeedProvider<DOMAINOBJECT> provider;


    public FeedResponse<DOMAINOBJECT> getFeedPage(String feedname, FeedRequest request) {
        int pageSize = getPageSize(request.getPageSize(), maxPageSize);
        String id = request.getSinceId();

        List<FeedElement<DOMAINOBJECT>> pageElements = provider
                .fetchData(id, pageSize)
                .sorted()
                .limit(pageSize)
                .collect(Collectors.toList());

        Set<String> ids = pageElements
                .stream()
                .map(FeedElement::getId)
                .collect(toSet());

        if (pageElements.size() != ids.size()) {
            // Found duplicate ids
            Event event = MetricsFactory.createEvent("feed.duplicateid");
            event.addTagToReport("feedname", feedname);
            event.report();

            LOG.warn("Found duplicate IDs in response to {} for feed {}", request, feedname);
            LOG.info("This can lead to excessive network usage between the producer and its consumers...");
        }

        if (pageElements.isEmpty()) {
            return new FeedResponse<DOMAINOBJECT>().setNextPageId(id);
        }

        String nextPageId = Optional.ofNullable(pageElements.get(pageElements.size() - 1))
                .map(FeedElement::getId)
                .orElse(null);

        return new FeedResponse<DOMAINOBJECT>().setNextPageId(nextPageId).setElements(pageElements);
    }

    public Map<String, Integer> activateWebhook() {
        return callbackUrls
                .stream()
                .collect(Collectors.toMap(Function.identity(), this::tryActivateWebHook));
    }

    private int tryActivateWebHook(String url) {
        try {
            Client client = ClientBuilder.newBuilder().build();
            Invocation.Builder request = client.target(url).request();
            this.interceptors.forEach(interceptor -> interceptor.apply(request));
            return request.build(HEAD).invoke().getStatus();
        } catch (Exception e) {
            LOG.error("Feil ved activate webhook til url {}, {}", url, e.getMessage());
            return 500;
        }
    }

    public boolean createWebhook(FeedWebhookRequest request) {
        return Optional.ofNullable(request.callbackUrl)
                .map(this::createWebhook)
                .orElseThrow(InvalidUrlException::new);
    }

    private boolean createWebhook(String callbackUrl) {
        if (callbackUrls.contains(callbackUrl)) {
            return false;
        }
        validateUrl(callbackUrl);

        callbackUrls.add(callbackUrl);

        return true;
    }

    private static int getPageSize(int pageSize, int maxPageSize) {
        return pageSize > maxPageSize ? maxPageSize : pageSize;
    }
}
