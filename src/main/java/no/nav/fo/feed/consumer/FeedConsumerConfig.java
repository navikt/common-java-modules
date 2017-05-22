package no.nav.fo.feed.consumer;

import java.util.ArrayList;
import java.util.List;

public class FeedConsumerConfig<DOMAINOBJECT> {
    Class<DOMAINOBJECT> domainobject;
    String lastEntry;
    String host;
    String feedName;
    String pollingInterval;
    String webhookPollingInterval;
    List<FeedCallback<DOMAINOBJECT>> callbacks = new ArrayList<>();

    public FeedConsumerConfig(Class<DOMAINOBJECT> domainobject, String lastEntry, String host, String feedName) {
        this.domainobject = domainobject;
        this.lastEntry = lastEntry;
        this.host = host;
        this.feedName = feedName;
    }

    public FeedConsumerConfig<DOMAINOBJECT> pollingInterval(String pollingInterval) {
        this.pollingInterval = pollingInterval;
        return this;
    }

    public FeedConsumerConfig<DOMAINOBJECT> webhookPollingInterval(String webhookPollingInterval) {
        this.webhookPollingInterval = webhookPollingInterval;
        return this;
    }

    public FeedConsumerConfig<DOMAINOBJECT> callback(FeedCallback callback) {
        if (!this.callbacks.contains(callback)) {
            this.callbacks.add(callback);
        }
        return this;
    }
}
