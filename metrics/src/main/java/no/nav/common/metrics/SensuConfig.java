package no.nav.common.metrics;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import static no.nav.common.utils.EnvironmentUtils.*;

@Value
@Builder
@Wither
public class SensuConfig {

    public static final long DEFAULT_SENSU_RETRY_INTERVAL = 5000;
    public static final int DEFAULT_SENSU_CONNECT_TIMEOUT = 3000;
    public static final int DEFAULT_SENSU_QUEUE_SIZE = 20_000;
    public static final long DEFAULT_SENSU_BATCH_TIME = 2000;
    public static final int DEFAULT_SENSU_BATCH_SIZE = 500;
    public static final boolean DEFAULT_SENSU_CLEANUP_ON_SHUTDOWN = true;

    private String sensuHost;
    private int sensuPort;
    private String application;
    private String hostname;
    private String cluster;
    private String namespace;

    /**
     *  How long we will wait before sending a new batch after a failure (in milliseconds)
     */
    private long retryInterval;

    /**
     * How long we will wait to connect to sensu before failing (in milliseconds)
     */
    private int connectTimeout;

    /**
     * How large the underlying queue where all reports are stored before being sent in batches to sensu
     */
    private int queueSize;

    /**
     * How long we will wait for a batch to build up before sending it to sensu (in milliseconds)
     */
    private long batchTime;

    /**
     * How many reports will be sent at once to sensu
     */
    private int batchSize;

    /**
     * If set to 'true' then a hook will be set up to flush metrics on shutdown
     */
    private boolean cleanupOnShutdown;

    public static SensuConfig defaultConfig() {
        return SensuConfig.builder()
                .sensuHost("sensu.nais")
                .sensuPort(3030)
                .application(requireApplicationName())
                .hostname(resolveHostName())
                .cluster(requireClusterName())
                .namespace(requireNamespace())
                .retryInterval(DEFAULT_SENSU_RETRY_INTERVAL)
                .connectTimeout(DEFAULT_SENSU_CONNECT_TIMEOUT)
                .queueSize(DEFAULT_SENSU_QUEUE_SIZE)
                .batchTime(DEFAULT_SENSU_BATCH_TIME)
                .batchSize(DEFAULT_SENSU_BATCH_SIZE)
                .cleanupOnShutdown(true)
                .build();
    }

}
