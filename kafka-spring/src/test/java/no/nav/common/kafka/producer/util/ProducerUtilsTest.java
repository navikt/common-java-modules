package no.nav.common.kafka.producer.util;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProducerUtilsTest {

    @Test
    public void skal_lage_null_record() {
        assertEquals(
                new ProducerRecord<String, String>("test", null),
                ProducerUtils.toJsonProducerRecord("test", null)
        );

        assertEquals(
                new ProducerRecord<String, String>("test", null, null),
                ProducerUtils.toJsonProducerRecord("test", null, null)
        );
    }

}
