package no.nav.fo.pact.example;

import au.com.dius.pact.consumer.*;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactSpecVersion;
import au.com.dius.pact.model.RequestResponsePact;
import no.nav.fo.pact.FOEndpoints;
import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;
import org.apache.http.client.fluent.Request;
import org.junit.*;

import java.util.Collections;

@Ignore
public class FOConsumerPactTest {

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
    public void runTest() {
        RequestResponsePact pact = createPact(new PactDslWithProvider(ConsumerPactBuilder.consumer("FOConsumer"), "FOProvider"));
        MockServer mockServer = new MockHttpServer(pact, MockProviderConfig.createDefault());
        PactVerificationResult result = mockServer.runAndWritePact(pact, PactSpecVersion.V3, server -> {
            Assert.assertEquals(Request.Get(server.getUrl() + FOEndpoints.IS_ALIVE_ENDPOINT).execute().returnResponse().getStatusLine().getStatusCode(), 200);
        });

        verifyPact(result);
    }

    private void verifyPact(PactVerificationResult result) {
        if (!result.equals(PactVerificationResult.Ok.INSTANCE)) {
            if (result instanceof PactVerificationResult.Error) {
                PactVerificationResult.Error error = (PactVerificationResult.Error) result;
                if (error.getMockServerState() != PactVerificationResult.Ok.INSTANCE) {
                    throw new AssertionError("Pact Test function failed with an exception, possibly due to " +
                            error.getMockServerState(), ((PactVerificationResult.Error) result).getError());
                } else {
                    throw new AssertionError("Pact Test function failed with an exception: " +
                            error.getError().getMessage(), error.getError());
                }
            } else {
                throw new PactMismatchesException(result);
            }
        }
    }

}
