package no.nav.fo.feed.producer;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import no.nav.fo.feed.exception.NoCallbackUrlException;
import no.nav.fo.feed.exception.NoWebhookUrlException;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.HttpMethod.HEAD;
import static no.nav.fo.feed.util.UrlValidator.validateUrl;
import static org.slf4j.LoggerFactory.getLogger;

@Data
@Accessors(chain = true)
public class FeedProducer<T> {

    private static final Logger LOG = getLogger(FeedProducer.class);

    private int maxPageSize;
    private Optional<String> webhookUrl;
    private Optional<String> callbackUrl;

    public Response getFeedPage(FeedRequest request, FeedProvider<T> feedProvider) {
        int pageSize = getPageSize(request.pageSize, maxPageSize);
        LocalDateTime sinceId = request.sinceId;

        List<FeedElement<T>> pageElements =
                feedProvider
                        .fetchData(sinceId, pageSize)
                        .limit(pageSize)
                        .collect(Collectors.toList());

        LocalDateTime nextPageId =
                pageElements
                        .stream()
                        .reduce((x, y) -> x.getId().isAfter(y.getId()) ? x : y)
                        .map(FeedElement::getId)
                        .orElseGet(null);

        FeedResponse<T> page = new FeedResponse<T>().setNextPageId(nextPageId).setElements(pageElements);

        return Response.ok().entity(page).build();
    }

    private static int getPageSize(int pageSize, int maxPageSize) {
        return pageSize > maxPageSize ? maxPageSize : pageSize;
    }

    public void activateWebhook() {
        webhookUrl.ifPresent(
                url -> {
                    Client client = ClientBuilder.newBuilder().build();
                    client.target(url).request().build(HEAD).invoke();
                }
        );
    }

    public Response getWebhook() {
        String url = webhookUrl.orElseThrow(NoWebhookUrlException::new);
        return Response.ok().entity(new FeedWebhookResponse().setWebhookUrl(url)).build();
    }

    public Response createWebhook(Optional<String> callbackUrl) {
        if (callbackUrl.equals(webhookUrl)) {
            return Response.ok().build();
        }
        String url = callbackUrl.orElseThrow(NoCallbackUrlException::new);
        validateUrl(url);

        webhookUrl = callbackUrl;
        return Response.created(getUri()).build();
    }

    @SneakyThrows
    private URI getUri() {
        return new URI("tilordninger/webhook");
    }
}
