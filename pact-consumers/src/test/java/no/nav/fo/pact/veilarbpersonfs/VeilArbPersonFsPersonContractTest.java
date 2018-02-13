package no.nav.fo.pact.veilarbpersonfs;

import au.com.dius.pact.consumer.ConsumerPactTestMk2;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import no.nav.fo.pact.FOApplication;
import no.nav.fo.pact.FOConsumerService;
import no.nav.fo.pact.FOEndpoints;
import org.json.JSONObject;
import org.junit.Assert;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;

public class VeilArbPersonFsPersonContractTest extends ConsumerPactTestMk2 {

    @Override
    protected RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = Collections.singletonMap("Content-Type", "application/json; charset=UTF-8");

        return builder
                .given("has a single person without children")
                .uponReceiving("a request for a single person without children")
                    .path(FOEndpoints.PERSON_API_URL + "/person/10108000398")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(headers)
                    .body(newJsonBody(body -> {
                        body.stringType("fornavn");
                        body.stringType("mellomnavn");
                        body.stringMatcher("kjonn", "K|M", "K");
                        body.date("fodselsdato", "yyyy-dd-mm");
                        body.stringMatcher("fodselsnummer", "[0-9]{11}", "17058932821");
                        body.stringType("statsborgerskap");
                        body.object("sivilstand", sivilstand -> {
                            sivilstand.stringType("sivilstand");
                            sivilstand.date("fraDato", "yyyy-dd-mm");
                        });
                        body.stringType("foobar");
                    }).build())
                .given("does not have person")
                .uponReceiving("a request for a person that does not exist")
                    .path(FOEndpoints.PERSON_API_URL + "/person/12345678901")
                    .method("GET")
                .willRespondWith()
                    .status(404)
                    .headers(headers)
                .toPact();
    }

    @Override
    protected String providerName() {
        return FOApplication.VEILARBPERSON.getFoName();
    }

    @Override
    protected String consumerName() {
        return FOApplication.VEILARBPERSONFS.getFoName();
    }

    @Override
    protected void runTest(MockServer mockServer) throws IOException {
//        try {
//            Thread.sleep(60000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        Response hasData = new FOConsumerService(mockServer.getUrl() + FOEndpoints.PERSON_API_URL + "/person/10108000398").getRequest().get();
        Assert.assertEquals(hasData.getStatus(), 200);
        JSONObject body = new JSONObject(hasData.readEntity(String.class));
        Assert.assertNotNull(body);
        Assert.assertTrue(body.getString("fodselsnummer").equals("17058932821"));

        Response noData = new FOConsumerService(mockServer.getUrl() + FOEndpoints.PERSON_API_URL + "/person/12345678901").getRequest().get();
        Assert.assertEquals(noData.getStatusInfo().getStatusCode(), 404);
    }
}
