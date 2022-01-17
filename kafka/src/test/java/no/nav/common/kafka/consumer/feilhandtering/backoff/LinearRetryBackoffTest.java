package no.nav.common.kafka.consumer.feilhandtering.backoff;

import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord;
import no.nav.common.kafka.consumer.feilhandtering.backoff.LinearBackoffStrategy;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;

public class LinearRetryBackoffTest {

    @Test
    public void getBackoffDuration__should_return_correct_value() {
        LinearBackoffStrategy backoff = new LinearBackoffStrategy(0, 100, 10);

        assertEquals(Duration.ofSeconds(0), backoff.getBackoffDuration(createTestRecord(0)));

        assertEquals(Duration.ofSeconds(10), backoff.getBackoffDuration(createTestRecord(1)));
        assertEquals(Duration.ofSeconds(30), backoff.getBackoffDuration(createTestRecord(3)));
        assertEquals(Duration.ofSeconds(90), backoff.getBackoffDuration(createTestRecord(9)));

        assertEquals(Duration.ofSeconds(100), backoff.getBackoffDuration(createTestRecord(10)));
        assertEquals(Duration.ofSeconds(100), backoff.getBackoffDuration(createTestRecord(100)));
    }

    private StoredConsumerRecord createTestRecord(int retries) {
        return new StoredConsumerRecord(
                1L,
                "",
                1,
                1L,
                null,
                null,
                "",
                retries,
                null,
                1L
        );
    }

}
