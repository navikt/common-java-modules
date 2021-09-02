package no.nav.common.kafka.utils;


import no.nav.common.kafka.util.KafkaUtils;
import org.apache.kafka.common.header.Headers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;

public class KafkaUtilsTest {

    @Test
    public void should_deserialize_object() {
        try {
            String testData = "[{\"key\":\"callId\",\"value\":\"YjY2YmM2NzdlZmJlNGZkODk2Nzk2NGZiYzgxZDUxY2U=\"}]";
            Headers headers = KafkaUtils.jsonToHeaders(testData);

            assertEquals(headers.toArray().length, 1);
        }catch (Exception e){
            fail();
        }

    }
}
