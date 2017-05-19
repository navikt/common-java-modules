package no.nav.fo.feed.producer;

import com.sun.java.browser.plugin2.DOM;
import javafx.beans.property.ReadOnlySetProperty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.common.FeedRequest;
import no.nav.fo.feed.common.FeedResponse;
import no.nav.fo.feed.common.FeedWebhookRequest;
import no.nav.fo.feed.exception.InvalidUrlException;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.HttpMethod.HEAD;
import static no.nav.fo.feed.util.UrlValidator.validateUrl;
import static org.slf4j.LoggerFactory.getLogger;

@Builder
public class FeedProducer<ID extends Comparable<ID>, DOMAINOBJECT> {

    private static final Logger LOG = getLogger(FeedProducer.class);

    @Builder.Default private int maxPageSize = 10000;
    private List<String> callbackUrls;
    private FeedProvider<ID, DOMAINOBJECT> provider;


    public FeedResponse<ID, DOMAINOBJECT> getFeedPage(FeedRequest request) {
        int pageSize = getPageSize(request.getPageSize(), maxPageSize);
        ID sinceId = (ID)request.getSinceId();

        List<FeedElement<ID, DOMAINOBJECT>> pageElements = provider
                .fetchData(sinceId, pageSize)
                .sorted()
                .limit(pageSize)
                .collect(Collectors.toList());

        if (pageElements.isEmpty()) {
            return new FeedResponse<ID, DOMAINOBJECT>().setNextPageId((ID)request.getSinceId());
        }

        ID nextPageId = Optional.ofNullable(pageElements.get(0))
                .map(FeedElement::getId)
                .orElse(null);

        return new FeedResponse<ID, DOMAINOBJECT>().setNextPageId(nextPageId).setElements(pageElements);
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
