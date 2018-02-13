package no.nav.fo.pact.global;

import au.com.dius.pact.consumer.ConsumerPactTestMk2;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import no.nav.fo.pact.FOConsumerService;
import no.nav.fo.pact.FOEndpoints;
import org.junit.Assert;

import javax.ws.rs.HttpMethod;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class IsAliveContract extends ConsumerPactTestMk2 {
    private String consumer;
    private String provider;
    private Map<String, String> headers;

    public IsAliveContract(String consumer, String provider) {
        this.consumer = consumer;
        this.provider = provider;
    }

    public IsAliveContract(String consumer, String provider, Map<String, String> headers) {
        this.consumer = consumer;
        this.provider = provider;
        this.headers = headers;
    }

    protected RequestResponsePact createPact(PactDslWithProvider builder) {
        String is_alive_state = "is alive state";
        String application_should_be_alive = "an is alive request";

        return builder
                .given(is_alive_state)
                .uponReceiving(application_should_be_alive)
                    .path(FOEndpoints.IS_ALIVE_ENDPOINT)
                    .method(HttpMethod.GET)
                .willRespondWith()
                    .status(200)
                    .headers(new HashMap<>(!Objects.isNull(headers) ? headers : Collections.emptyMap()))
                    .body("Application: UP")
                .toPact();
    }

    protected void runTest(MockServer mockServer) {
        Assert.assertEquals(new FOConsumerService(mockServer.getUrl() + FOEndpoints.IS_ALIVE_ENDPOINT).isAlive(), true);
    }

    protected String providerName() {
        return provider;
    }

    protected String consumerName() {
        return consumer;
    }

    protected void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
