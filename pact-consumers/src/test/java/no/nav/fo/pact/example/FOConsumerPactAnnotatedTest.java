package no.nav.fo.pact.example;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactSpecVersion;
import au.com.dius.pact.model.RequestResponsePact;
import no.nav.fo.pact.FOConsumerService;
import no.nav.fo.pact.FOEndpoints;
import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;
import org.junit.*;

import java.util.Collections;

@Ignore
public class FOConsumerPactAnnotatedTest {

    @Rule
    public PactProviderRuleMk2 provider = new PactProviderRuleMk2("FOProvider", "localhost", 4682,  PactSpecVersion.V3, this);

    @Before
    public void setUp() {
        SSLTestUtils.disableCertificateChecks();
    }

    @Pact(consumer = "FOConsumer", provider = "FOProvider")
    protected RequestResponsePact createPact(PactDslWithProvider builder) {
        RequestResponsePact pact = builder
                .given("is alive state") // NOTE: Using provider states are optional, you can leave it out
                .uponReceiving("application should be alive")
                    .path(FOEndpoints.IS_ALIVE_ENDPOINT)
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(Collections.singletonMap("Content-Type", "text/html; charset=UTF-8"))
                    .body("Application: UP")
                .toPact();
        return pact;
    }

    @Test
    @PactVerification(value = "FOProvider", fragment = "createPact")
    public void shouldBeAlive() {
        Assert.assertEquals(new FOConsumerService("http://localhost:4682" + FOEndpoints.IS_ALIVE_ENDPOINT).isAlive(), true);

        // With Fluent Apache HTTP lib
        //Assert.assertEquals(Request.Get("http://localhost:4682" + FOEndpoints.IS_ALIVE_ENDPOINT).execute().returnResponse().getStatusLine().getStatusCode(), true);
    }

}
