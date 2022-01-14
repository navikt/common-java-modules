package no.nav.common.kafka.consumer.feilhandtering.backoff;

import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord;
import no.nav.common.utils.MathUtils;

import java.time.Duration;

/**
 * Linear backoff strategy. Returns a duration between "minBackoffSeconds" and "maxBackoffSeconds" based on how many previous retries
 * the record has.
 */
public class LinearBackoffStrategy implements BackoffStrategy {

    private final int minBackoffSeconds;

    private final int maxBackoffSeconds;

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
