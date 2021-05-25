package no.nav.common.kafka.producer;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.AuthenticationException;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RollingCredentialsKafkaProducerClientTest {

    @Test
    public void should_create_new_client_after_authentication_exception() {
        KafkaProducerClient<String, String> client = spy(new KafkaProducerClient<String, String>() {
            @Override
            public void close() {}

            @Override
            public RecordMetadata sendSync(ProducerRecord<String, String> record) {
                return null;
            }

            @Override
            public Future<RecordMetadata> send(ProducerRecord<String, String> record) {
                return null;
            }

            @Override
            public Future<RecordMetadata> send(ProducerRecord<String, String> record, Callback callback) {
                AuthenticationException ae = new AuthenticationException("Auth failed");
                if (callback != null) {
                    callback.onCompletion(null, ae);
                }
                return CompletableFuture.failedFuture(ae);
            }

            @Override
            public Producer<String, String> getProducer() {
                return null;
            }
        });

        Supplier<KafkaProducerClient<String, String>> supplier = () -> client;

        RollingCredentialsKafkaProducerClient<String, String> rollingCredsClient = new RollingCredentialsKafkaProducerClient<>(supplier);

        ProducerRecord<String, String> record = new ProducerRecord<>("test", "test");

        try {
            rollingCredsClient.sendSync(record);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof AuthenticationException);
        }

        verify(client, times(2)).send(any(), any());
    }

}
