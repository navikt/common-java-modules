package no.nav.fo.pact.example;

import au.com.dius.pact.consumer.ConsumerPactTestMk2;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import no.nav.fo.pact.FOConsumerService;
import no.nav.fo.pact.FOEndpoints;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

@Ignore
public class FOConsumerPactBaseTest extends ConsumerPactTestMk2 {

    protected RequestResponsePact createPact(PactDslWithProvider builder) {
        return builder
                .given("is alive state") // NOTE: Using provider states are optional, you can leave it out
                .uponReceiving("application should be alive")
                    .path(FOEndpoints.IS_ALIVE_ENDPOINT)
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(Collections.singletonMap("Content-Type", "text/html; charset=UTF-8"))
                .body("Application: UP")
                .toPact();
    }

    protected String providerName() {
        return "FOProvider";
    }

    protected String consumerName() {
        return "FOConsumer";
    }

    protected void runTest(MockServer mockServer) {
        Assert.assertEquals(new FOConsumerService(mockServer.getUrl() + FOEndpoints.IS_ALIVE_ENDPOINT).isAlive(), true);
    }
}
