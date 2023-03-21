package no.nav.common.metrics;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import static no.nav.common.utils.EnvironmentUtils.*;

@With
@Value
@Builder
public class SensuConfig {

    public static final long DEFAULT_SENSU_RETRY_INTERVAL = 5000;
    public static final int DEFAULT_SENSU_CONNECT_TIMEOUT = 3000;
    public static final int DEFAULT_SENSU_QUEUE_SIZE = 20_000;
    public static final long DEFAULT_SENSU_MAX_BATCH_TIME = 10000;
    public static final int DEFAULT_SENSU_BATCH_SIZE = 500;
    public static final boolean DEFAULT_SENSU_CLEANUP_ON_SHUTDOWN = true;

    public static final long SENSU_MIN_BATCH_TIME = 100;
    public static final long SENSU_MIN_QUEUE_SIZE = 100;

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
     * Maximum time we will wait for a batch to build up before sending it to sensu (in milliseconds)
     */
    private long maxBatchTime;

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
                .maxBatchTime(DEFAULT_SENSU_MAX_BATCH_TIME)
                .batchSize(DEFAULT_SENSU_BATCH_SIZE)
                .cleanupOnShutdown(true)
                .build();
    }

}
