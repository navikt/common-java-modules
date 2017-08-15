package no.nav.fo.feed.producer;

import lombok.Builder;
import no.nav.fo.feed.common.*;
import no.nav.fo.feed.exception.InvalidUrlException;
import no.nav.fo.feed.util.MetricsUtils;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.HttpMethod.HEAD;
import static no.nav.fo.feed.util.RestUtils.getClient;
import static no.nav.fo.feed.util.UrlValidator.validateUrl;
import static org.slf4j.LoggerFactory.getLogger;

@Builder
public class FeedProducer<DOMAINOBJECT extends Comparable<DOMAINOBJECT>> implements Authorization {

    private static final Logger LOG = getLogger(FeedProducer.class);

    @Builder.Default
    private int maxPageSize = 10000;

    // Trådsikkert Set. Er final slik at ikke brukere av FeedProducer kan velge å sette inn et annet Set som 
    // ikke er trådsikkert. Det bør ikke være noen grunn til at de som bruker FeedProducer skal behøve å forholde 
    // seg direkte til dette Set-et.
    private final Set<String> callbackUrls = ConcurrentHashMap.newKeySet();

    @Builder.Default
    private List<OutInterceptor> interceptors = new ArrayList<>();
    @Builder.Default
    private FeedAuthorizationModule authorizationModule = (feedname) -> true;
    private FeedProvider<DOMAINOBJECT> provider;


    public FeedResponse<DOMAINOBJECT> getFeedPage(String feedname, FeedRequest request) {
        int pageSize = getPageSize(request.getPageSize(), maxPageSize);
        String id = request.getSinceId();

        List<FeedElement<DOMAINOBJECT>> pageElements = provider
                .fetchData(id, pageSize)
                .sorted()
                .limit(pageSize + 1) // se fetchnotlimited
                .collect(Collectors.toList());

        if (pageElements.size() > pageSize) {
            MetricsUtils.metricEvent("fetchnotlimited", feedname);
            LOG.warn("Provider retrieved more than <pageSize> elements in response to {} for feed {}", request, feedname);
            LOG.info("This can lead to excessive resource consumption by the producer...");
        }

        Set<String> ids = pageElements
                .stream()
                .map(FeedElement::getId)
                .collect(toSet());

        if (pageElements.size() != ids.size()) {
            MetricsUtils.metricEvent("duplicateid", feedname);
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
            Client client = getClient();
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

    @Override
    public FeedAuthorizationModule getAuthorizationModule() {
        return authorizationModule;
    }

    private boolean createWebhook(String callbackUrl) {
        validateUrl(callbackUrl);
        return callbackUrls.add(callbackUrl);
    }

    private static int getPageSize(int pageSize, int maxPageSize) {
        return pageSize > maxPageSize ? maxPageSize : pageSize;
    }
}
