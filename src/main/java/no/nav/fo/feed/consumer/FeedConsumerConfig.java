package no.nav.fo.feed.consumer;

import no.nav.fo.feed.common.OutInterceptor;

import java.util.List;
import java.util.function.Supplier;

public class FeedConsumerConfig<DOMAINOBJECT> {
    Class<DOMAINOBJECT> domainobject;
    Supplier<String> lastEntrySupplier;
    String host;
    String feedName;
    String pollingInterval;
    String webhookPollingInterval;
    FeedCallback<DOMAINOBJECT> callback;
    List<OutInterceptor> interceptors;
    int pageSize;

    public FeedConsumerConfig(Class<DOMAINOBJECT> domainobject, Supplier<String> lastEntrySupplier, String host, String feedName) {
        this.domainobject = domainobject;
        this.lastEntrySupplier = lastEntrySupplier;
        this.host = host;
        this.feedName = feedName;
        this.pageSize = 100;
    }

    public FeedConsumerConfig<DOMAINOBJECT> interceptors(List<OutInterceptor> interceptors) {
        this.interceptors = interceptors;
        return this;
    }

    public FeedConsumerConfig<DOMAINOBJECT> pollingInterval(String pollingInterval) {
        this.pollingInterval = pollingInterval;
        return this;
    }

    public FeedConsumerConfig<DOMAINOBJECT> webhookPollingInterval(String webhookPollingInterval) {
        this.webhookPollingInterval = webhookPollingInterval;
        return this;
    }

    public FeedConsumerConfig<DOMAINOBJECT> callback(FeedCallback<DOMAINOBJECT> callback) {
        this.callback = callback;
        return this;
    }

    public FeedConsumerConfig<DOMAINOBJECT> pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }
}
