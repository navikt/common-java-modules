package no.nav.fo.feed.consumer;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.feed.common.FeedAuthorizationModule;
import no.nav.fo.feed.common.OutInterceptor;
import org.quartz.ScheduleBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

public class FeedConsumerConfig<DOMAINOBJECT> {

    public final Class<DOMAINOBJECT> domainobject;
    public final Supplier<String> lastEntrySupplier;
    public final String host;
    public final String feedName;

    public final ScheduleCreator pollingConfig;
    public final WebhookScheduleCreator webhookPollingConfig;

    FeedCallback<DOMAINOBJECT> callback;
    List<OutInterceptor> interceptors = new ArrayList<>();
    FeedAuthorizationModule authorizationModule = (feedname) -> true;
    int pageSize;

    LockingTaskExecutor lockExecutor;
    int lockHoldingLimitInMilliSeconds;


    public FeedConsumerConfig(BaseConfig<DOMAINOBJECT> baseConfig, ScheduleCreator pollingConfig) {
        this(baseConfig, pollingConfig, null);
    }

    public FeedConsumerConfig(BaseConfig<DOMAINOBJECT> baseConfig, ScheduleCreator pollingConfig, WebhookScheduleCreator webhookPollingConfig) {
        this.domainobject = baseConfig.domainobject;
        this.lastEntrySupplier = baseConfig.lastEntrySupplier;
        this.host = baseConfig.host;
        this.feedName = baseConfig.feedName;
        this.pollingConfig = pollingConfig;
        this.webhookPollingConfig = webhookPollingConfig;

        this.pageSize = 100;
    }

    public FeedConsumerConfig<DOMAINOBJECT> authorizatioModule(FeedAuthorizationModule authorizationModule) {
        this.authorizationModule = authorizationModule;
        return this;
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

    public FeedConsumerConfig<DOMAINOBJECT> lockProvider(LockProvider lockProvider, int lockHoldingLimitInMilliSeconds) {
        return lockExecutor(new DefaultLockingTaskExecutor(lockProvider), lockHoldingLimitInMilliSeconds);
    }

    public FeedConsumerConfig<DOMAINOBJECT> lockExecutor(LockingTaskExecutor lockExecutor, int lockHoldingLimitInMilliSeconds) {
        this.lockExecutor = lockExecutor;
        this.lockHoldingLimitInMilliSeconds = lockHoldingLimitInMilliSeconds;
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

    public static class ScheduleCreator {
        public final ScheduleBuilder<?> scheduleBuilder;

        public ScheduleCreator(ScheduleBuilder<?> scheduleBuilder) {
            this.scheduleBuilder = scheduleBuilder;
        }
    }
    public static class WebhookScheduleCreator extends ScheduleCreator {
        public final String apiRootPath;

        public WebhookScheduleCreator(ScheduleBuilder<?> builder, String apiRootPath) {
            super(builder);
            this.apiRootPath = apiRootPath;
        }
    }

    public static class CronPollingConfig extends ScheduleCreator {
        public CronPollingConfig(String pollingInterval) {
            super(cronSchedule(pollingInterval));
        }
    }

    public static class SimplePollingConfig extends ScheduleCreator {
        public SimplePollingConfig(int pollingIntervalInSeconds) {
            super(simpleSchedule().withIntervalInSeconds(pollingIntervalInSeconds).repeatForever());
        }
    }

    public static class CronWebhookPollingConfig extends WebhookScheduleCreator {
        public CronWebhookPollingConfig(String webhookPollingInterval, String apiRootPath) {
            super(cronSchedule(webhookPollingInterval), apiRootPath);
        }
    }

    public static class SimpleWebhookPollingConfig extends WebhookScheduleCreator {
        public SimpleWebhookPollingConfig(int webhookPollingIntervalInSeconds, String apiRootPath) {
            super(simpleSchedule().withIntervalInSeconds(webhookPollingIntervalInSeconds).repeatForever(), apiRootPath);
        }
    }
}
