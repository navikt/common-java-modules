package no.nav.common.kafka.consumer.feilhandtering.backoff;

import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord;
import no.nav.common.utils.MathUtils;

import java.time.Duration;

/**
 * Linear backoff strategy. Returns a duration between "minBackoffSeconds" and "maxBackoffSeconds" based on how many previous retries
 * the record has.
 *
 * Ex: minBackoffSeconds=0, maxBackoffSeconds=100, maxBackoffAfterRetries=5
 * previous retries for record = 0 -> backoff time 0s
 * previous retries for record = 1 -> backoff time 20s
 * previous retries for record = 2 -> backoff time 40s
 * previous retries for record = 3 -> backoff time 60s
 * previous retries for record = 4 -> backoff time 80s
 * previous retries for record = 5 -> backoff time 100s
 * previous retries for record = 100 -> backoff time 100s
 */
public class LinearBackoffStrategy implements BackoffStrategy {

    /**
     * The minimum backoff that can be returned
     */
    private final int minBackoffSeconds;

    /**
     * The maximum backoff that can be returned
     */
    private final int maxBackoffSeconds;

    /**
     * How many retries until the max backoff is reached
     */
    private final int maxBackoffAfterRetries;

    public LinearBackoffStrategy(int minBackoffSeconds, int maxBackoffSeconds, int maxBackoffAfterRetries) {
        this.minBackoffSeconds = minBackoffSeconds;
        this.maxBackoffSeconds = maxBackoffSeconds;
        this.maxBackoffAfterRetries = maxBackoffAfterRetries;
    }

    @Override
    public Duration getBackoffDuration(StoredConsumerRecord record) {
        int clampedRetry = MathUtils.clamp(record.getRetries(), 0, maxBackoffAfterRetries);
        float percentBackoff = clampedRetry / (float) maxBackoffAfterRetries;
        long secondsBackoff = MathUtils.linearInterpolation(minBackoffSeconds, maxBackoffSeconds, percentBackoff);

        return Duration.ofSeconds(secondsBackoff);
    }

}
