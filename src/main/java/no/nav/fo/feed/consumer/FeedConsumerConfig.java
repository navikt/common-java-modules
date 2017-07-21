package no.nav.fo.feed.consumer;

import no.nav.fo.feed.common.OutInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FeedConsumerConfig<DOMAINOBJECT> {

    public final Class<DOMAINOBJECT> domainobject;
    public final Supplier<String> lastEntrySupplier;
    public final String host;
    public final String feedName;

    public final String pollingInterval;
    public final String webhookPollingInterval;
    public final String apiRootPath;

    FeedCallback<DOMAINOBJECT> callback;
    List<OutInterceptor> interceptors = new ArrayList<>();
    int pageSize;

    public FeedConsumerConfig(BaseConfig<DOMAINOBJECT> baseConfig, PollingConfig pollingConfig) {
        this(baseConfig, pollingConfig, null);
    }

    public FeedConsumerConfig(BaseConfig<DOMAINOBJECT> baseConfig, WebhookPollingConfig webhookPollingConfig) {
        this(baseConfig, null, webhookPollingConfig);
    }

    public FeedConsumerConfig(BaseConfig<DOMAINOBJECT> baseConfig, PollingConfig pollingConfig, WebhookPollingConfig webhookPollingConfig) {
        this.domainobject = baseConfig.domainobject;
        this.lastEntrySupplier = baseConfig.lastEntrySupplier;
        this.host = baseConfig.host;
        this.feedName = baseConfig.feedName;

        if (pollingConfig != null) {
            this.pollingInterval = pollingConfig.pollingInterval;
        } else {
            this.pollingInterval = null;
        }

        if (webhookPollingConfig != null) {
            this.webhookPollingInterval = webhookPollingConfig.webhookPollingInterval;
            this.apiRootPath = webhookPollingConfig.apiRootPath;
        } else {
            this.webhookPollingInterval = null;
            this.apiRootPath = null;
        }

        this.pageSize = 100;
    }

    public FeedConsumerConfig<DOMAINOBJECT> interceptors(List<OutInterceptor> interceptors) {
        this.interceptors = interceptors;
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

    public static class BaseConfig<DOMAINOBJECT> {
        public final Class<DOMAINOBJECT> domainobject;
        public final Supplier<String> lastEntrySupplier;
        public final String host;
        public final String feedName;

        public BaseConfig(Class<DOMAINOBJECT> domainobject, Supplier<String> lastEntrySupplier, String host, String feedName) {
            this.domainobject = domainobject;
            this.lastEntrySupplier = lastEntrySupplier;
            this.host = host;
            this.feedName = feedName;
        }
    }

    public static class PollingConfig {
        public final String pollingInterval;

        public PollingConfig(String pollingInterval) {
            this.pollingInterval = pollingInterval;
        }
    }

    public static class WebhookPollingConfig {
        public final String webhookPollingInterval;
        public final String apiRootPath;

        public WebhookPollingConfig(String webhookPollingInterval, String apiRootPath) {
            this.webhookPollingInterval = webhookPollingInterval;
            this.apiRootPath = apiRootPath;
        }
    }

}
