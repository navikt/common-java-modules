package no.nav.fo.feed.producer;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

import javax.ws.rs.client.Invocation;

import org.junit.Test;

import no.nav.fo.feed.common.FeedWebhookRequest;
import no.nav.fo.feed.common.OutInterceptor;

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
    public void activateWebhookErAsynkron() throws InterruptedException {
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
        producer.activateWebhook();

        // Sjekker at kallet til activateWebHook blir ferdig "med det samme" - dvs. før de faktiske prosesseringene av kall
        // til webhookene er ferdig
        assertThat(new Long(System.currentTimeMillis() - startTime), lessThan(sleepTime));

        // Sover for å gi trådene tid til å kalle interceptorene, og verifiserer deretter at interceptoren har blitt kalt 
        // to ganger (én for hver webhook-url)
        Thread.sleep(sleepTime + 500);
        verify(outInterceptor, times(2)).apply(any(Invocation.Builder.class));
    }
}
