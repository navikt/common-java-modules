package no.nav.fo.feed.consumer;

import lombok.Builder;
import lombok.SneakyThrows;
import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.common.FeedResponse;
import no.nav.fo.feed.common.FeedWebhookRequest;
import no.nav.metrics.aspects.Timed;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.slf4j.LoggerFactory.getLogger;

@Builder
public class FeedConsumer<ID extends Comparable<ID>, DOMAINOBJECT> implements Job {
    public static String applicationContextroot;
    private static final Logger LOG = getLogger(FeedConsumer.class);
    private static Scheduler scheduler;

    static {
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            throw new RuntimeException("Could not create schduler", e);
        }
    }

    private ID lastEntry;
    private String host;
    private String feedName;
    @Builder.Default private String pollingInterval = "0 * * * * ?";
    @Builder.Default private String webhookPollingInterval = "0 * * * * ?";
    @Builder.Default private boolean allowWebhooks = false;
    private List<FeedCallback<DOMAINOBJECT>> callbacks;

    public FeedConsumer(ID lastEntry, String host, String feedName, String pollingInterval, String webhookPollingInterval, boolean allowWebhooks, List<FeedCallback<DOMAINOBJECT>> callbacks) {
        this.lastEntry = lastEntry;
        this.host = host;
        this.feedName = feedName;
        this.pollingInterval = pollingInterval;
        this.webhookPollingInterval = webhookPollingInterval;
        this.allowWebhooks = allowWebhooks;
        this.callbacks = callbacks;

        this.init();
    }

    @SneakyThrows
    private void init() {
        JobDetail pollingJob = newJob(FeedConsumer.class)
                .withIdentity(feedName, host)
                .build();
        CronTrigger pollingTrigger = newTrigger()
                .withIdentity(feedName, host)
                .withSchedule(cronSchedule(this.pollingInterval))
                .build();
        scheduler.scheduleJob(pollingJob, pollingTrigger);

        if (allowWebhooks) {
            JobDetail webhookJob = newJob(FeedConsumer.class)
                    .withIdentity(feedName + "/webhook", host)
                    .build();
            CronTrigger webhookTrigger = newTrigger()
                    .withIdentity(feedName + "/webhook", host)
                    .withSchedule(cronSchedule(this.webhookPollingInterval))
                    .build();
            scheduler.scheduleJob(webhookJob, webhookTrigger);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

    }

    public boolean webhookCallback() {
        if (!allowWebhooks) {
            return false;
        }

        // TODO Fetch data
        poll(lastEntry, 1000);

        return true;
    }

    public void addCallback(FeedCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    @Timed(name = "feed.registerWebhook")
    public void registerWebhook() {
        Client client = ClientBuilder.newBuilder().build();
        FeedWebhookRequest body = new FeedWebhookRequest().setCallbackUrl(callbackUrl());
        Entity<FeedWebhookRequest> entity = Entity.entity(body, APPLICATION_JSON_TYPE);
        Response response = client
                .target(asUrl(host + feedName + "/webhook"))
                .request()
                .buildPut(entity)
                .invoke();

        if (response.getStatus() == 201) {
            LOG.info("Webhook opprettet hos produsent!");
        } else if (response.getStatus() != 200) {
            LOG.warn("Endepunkt for opprettelse av webhook returnerte feilkode {}", response.getStatus());
        }
    }

    @Timed(name = "feed.poll")
    public void poll(ID sinceId, int pageSize) {
        Client client = ClientBuilder.newBuilder().build();
        Response response = client
                .target(asUrl(host + feedName))
                .queryParam("since_id", sinceId)
                .request()
                .buildGet()
                .invoke();

        FeedResponse<ID, DOMAINOBJECT> entity = (FeedResponse<ID, DOMAINOBJECT>) response.getEntity();// FeedResponse<ID, DOMAINOBJECT>
        List<DOMAINOBJECT> data = entity
                .getElements()
                .stream()
                .map(FeedElement::getElement)
                .collect(Collectors.toList());

        if (response.getStatus() != 200) {
            LOG.warn("Endepunkt for polling av feed returnerte feilkode {}", response.getStatus());
        }

        callbacks.forEach((callback) -> callback.callback(data));
        lastEntry = entity.getNextPageId();
    }

    private String callbackUrl() {
        String environmentClass = System.getProperty("environment.class");
        String environment = System.getProperty("environment.name");
        String host = getHost(environmentClass, environment);

        return asUrl(format("%s/%s/feed/%s", host, applicationContextroot, feedName));
    }

    private String getHost(String environmentClass, String environment) {
        switch (environmentClass) {
            case "u":
            case "t":
            case "q": {
                return format("https://app-%s.adeo.no", environment);
            }
            default: {
                return "https://app.adeo.no";
            }
        }
    }

    private String asUrl(String s) {
        return s;
    }

}
