package no.nav.common.feed.producer;

import no.nav.common.feed.common.FeedWebhookRequest;
import no.nav.common.feed.common.OutInterceptor;
import org.junit.Test;

import javax.ws.rs.client.Invocation;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.*;

public class FeedProducerTest {

    public class MittObjekt implements Comparable<MittObjekt> {

        @Override
        public int compareTo(MittObjekt arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

    }

    @Test
    public void createWebhookReturnererTrueForUlikeUrler() {
        FeedProducer<MittObjekt> producer = FeedProducer.<MittObjekt>builder().build();
        assertThat(producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url1")), is(true));
        assertThat(producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url2")), is(true));
    }
    
    @Test
    public void createWebhookReturnererFalseHvisURLFinnesFraFor() {
        FeedProducer<MittObjekt> producer = FeedProducer.<MittObjekt>builder().build();
        assertThat(producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url1")), is(true));
        assertThat(producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url1")), is(false));
    }

    @Test
    public void activateWebhookFungerer() {
        FeedProducer<MittObjekt> producer = FeedProducer.<MittObjekt>builder().build();
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url1"));
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url1"));
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url2"));
        producer.activateWebhook();
    }

    @Test
    public void activateWebhookErAsynkron() throws InterruptedException, ExecutionException {
        // Bruker en mock interceptor både for å sikre at prosessering av webhookene tar litt tid og til å 
        // verifisere at de har blitt prosessert.
        OutInterceptor outInterceptor = mock(OutInterceptor.class);
        Long sleepTime = new Long(50);
        doAnswer(invocation -> {
            Thread.sleep(sleepTime);
            return null;
        }).when(outInterceptor).apply(any(Invocation.Builder.class));

        FeedProducer<MittObjekt> producer = FeedProducer.<MittObjekt>builder().interceptors(asList(outInterceptor)).build();
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url1"));
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url2"));

        long startTime = System.currentTimeMillis();
        Map<String, Future<Integer>> responses = producer.activateWebhook();

        // Sjekker at kallet til activateWebHook blir ferdig "med det samme" - dvs. før de faktiske prosesseringene av kall
        // til webhookene er ferdig
        assertThat(new Long(System.currentTimeMillis() - startTime), lessThan(sleepTime));

        assertThat(responses.get("http://url1").get(), is(500));
        assertThat(responses.get("http://url2").get(), is(500));
        verify(outInterceptor, times(2)).apply(any(Invocation.Builder.class));
    }
}