package no.nav.fo.feed.producer;

import lombok.Builder;
import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.common.FeedRequest;
import no.nav.fo.feed.common.FeedResponse;
import no.nav.fo.feed.common.FeedWebhookRequest;
import no.nav.fo.feed.exception.InvalidUrlException;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.HttpMethod.HEAD;
import static no.nav.fo.feed.util.UrlValidator.validateUrl;
import static org.slf4j.LoggerFactory.getLogger;

@Builder
public class FeedProducer<DOMAINOBJECT extends Comparable<DOMAINOBJECT>> {

    private static final Logger LOG = getLogger(FeedProducer.class);

    @Builder.Default private int maxPageSize = 10000;
    @Builder.Default private List<String> callbackUrls = new ArrayList<>();
    private FeedProvider<DOMAINOBJECT> provider;


    public FeedResponse<DOMAINOBJECT> getFeedPage(FeedRequest request) {
        int pageSize = getPageSize(request.getPageSize(), maxPageSize);
        String id = request.getSinceId();

        List<FeedElement<DOMAINOBJECT>> pageElements = provider
                .fetchData(id, pageSize)
                .sorted()
                .limit(pageSize)
                .collect(Collectors.toList());

        if (pageElements.isEmpty()) {
            return new FeedResponse<DOMAINOBJECT>().setNextPageId(id);
        }

        String nextPageId = Optional.ofNullable(pageElements.get(pageElements.size() - 1))
                .map(FeedElement::getId)
                .orElse(null);

        return new FeedResponse<DOMAINOBJECT>().setNextPageId(nextPageId).setElements(pageElements);
    }

    public void activateWebhook() {
        callbackUrls
                .forEach((url) -> {
                    Client client = ClientBuilder.newBuilder().build();
                    client.target(url).request().build(HEAD).invoke();
                });
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
