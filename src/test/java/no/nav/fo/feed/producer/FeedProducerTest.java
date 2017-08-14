package no.nav.fo.feed.producer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.Test;

import no.nav.fo.feed.common.FeedWebhookRequest;

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
        System.out.println(ReflectionToStringBuilder.toString(producer));
        assertThat(producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url1")), is(true));
        assertThat(producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url1")), is(false));
    }

    @Test
    public void activateWebhookFungerer() {
        FeedProducer<MittObjekt> producer = FeedProducer.<MittObjekt>builder().build();
        System.out.println(ReflectionToStringBuilder.toString(producer));
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url1"));
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url1"));
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl("http://url2"));
        producer.activateWebhook();
    }
}
