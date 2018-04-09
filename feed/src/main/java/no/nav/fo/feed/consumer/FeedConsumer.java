package no.nav.fo.feed.consumer;

import net.javacrumbs.shedlock.core.LockConfiguration;
import no.nav.fo.feed.common.*;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.rest.RestUtils;
import org.slf4j.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static no.nav.fo.feed.consumer.FeedPoller.createScheduledJob;
import static no.nav.fo.feed.util.UrlUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

public class FeedConsumer<DOMAINOBJECT extends Comparable<DOMAINOBJECT>> implements Pingable, Authorization, ApplicationListener<ContextClosedEvent> {
    private static final Logger LOG = getLogger(FeedConsumer.class);

    private final FeedConsumerConfig<DOMAINOBJECT> config;
    private final Ping.PingMetadata pingMetadata;
    private int lastResponseHash;

    private static final Client REST_CLIENT = RestUtils.createClient();

    public FeedConsumer(FeedConsumerConfig<DOMAINOBJECT> config) {
        String feedName = config.feedName;
        String host = config.host;

        this.config = config;
        this.pingMetadata = new Ping.PingMetadata(getTargetUrl(), String.format("feed-consumer av '%s'", feedName), false);

        createScheduledJob(feedName, host, config.pollingConfig, runWithLock(feedName, this::poll));
        createScheduledJob(feedName + "/webhook", host, config.webhookPollingConfig, this::registerWebhook);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        FeedPoller.shutdown();
    }

    public boolean webhookCallback() {
        if (this.config.webhookPollingConfig == null) {
            return false;
        }

        CompletableFuture.runAsync(runWithLock(this.config.feedName, this::poll));
        return true;
    }

    public void addCallback(FeedCallback callback) {
        this.config.callback(callback);
    }

    void registerWebhook() {
        String callbackUrl = callbackUrl(this.config.webhookPollingConfig.apiRootPath, this.config.feedName);
        FeedWebhookRequest body = new FeedWebhookRequest().setCallbackUrl(callbackUrl);

        Entity<FeedWebhookRequest> entity = Entity.entity(body, APPLICATION_JSON_TYPE);

        Invocation.Builder request = REST_CLIENT
                .target(asUrl(this.config.host, "feed", this.config.feedName, "webhook"))
                .request();

        config.interceptors.forEach(interceptor -> interceptor.apply(request));

        Response response = request
                .buildPut(entity)
                .invoke();

        int responseStatus = response.getStatus();
        if (responseStatus == 201) {
            LOG.info("Webhook opprettet hos produsent!");
        } else if (responseStatus != 200) {
            LOG.warn("Endepunkt for opprettelse av webhook returnerte feilkode {}", responseStatus);
        }
    }

    public synchronized Response poll() {
        Response response = fetchChanges();

        if (response.getStatus() != 200) {
            LOG.warn("Endepunkt for polling av feed returnerte feilkode {}", response.getStatus());
        } else {
            ParameterizedType type = new FeedParameterizedType(this.config.domainobject);
            FeedResponse<DOMAINOBJECT> entity = response.readEntity(new GenericType<>(type));
            List<FeedElement<DOMAINOBJECT>> elements = entity.getElements();
            if (elements != null && !elements.isEmpty()) {
                List<DOMAINOBJECT> data = elements
                        .stream()
                        .map(FeedElement::getElement)
                        .collect(Collectors.toList());

                if (!(entity.hashCode() == lastResponseHash)) {
                    this.config.callback.call(entity.getNextPageId(), data);
                }
                this.lastResponseHash = entity.hashCode();
            }
        }

        return response;
    }

    Response fetchChanges() {
        String lastEntry = this.config.lastEntrySupplier.get();
        Invocation.Builder request = REST_CLIENT
                .target(getTargetUrl())
                .queryParam(QUERY_PARAM_ID, lastEntry)
                .queryParam(QUERY_PARAM_PAGE_SIZE, this.config.pageSize)
                .request();

        config.interceptors.forEach(interceptor -> interceptor.apply(request));

        return request
                .buildGet()
                .invoke();
    }

    private String getTargetUrl() {
        return asUrl(this.config.host, "feed", this.config.feedName);
    }

    @Override
    public Ping ping() {
        try {
            int status = fetchChanges().getStatus();
            if (status == 200) {
                return Ping.lyktes(pingMetadata);
            } else {
                return Ping.feilet(pingMetadata, "HTTP status " + status);
            }
        } catch (Throwable e) {
            return Ping.feilet(pingMetadata, e);
        }
    }

    @Override
    public FeedAuthorizationModule getAuthorizationModule() {
        return config.authorizationModule;
    }

    private Runnable runWithLock(String lockname, Runnable task) {
        return () -> {
            if (this.config.lockExecutor == null) {
                task.run();
            } else {
                Instant lockAtMostUntil = Instant.now().plusMillis(this.config.lockHoldingLimitInMilliSeconds);
                LockConfiguration lockConfiguration = new LockConfiguration(lockname, lockAtMostUntil);
                this.config.lockExecutor.executeWithLock(task, lockConfiguration);
            }
        };
    }
}
