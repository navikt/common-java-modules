package no.nav.common.client.pdl;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.client.TestUtils;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static org.junit.Assert.assertEquals;

public class PdlAktorOppslagClientImpl {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/pdl/";

    @Test
    public void rawRequest__skal_lage_riktig_request_og_forwarde_respons() {
        String graphqlJsonRequest = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "pdl-client-graphql-request.json");
        String graphqlJsonResponse = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "pdl-client-graphql-response.json");

        String apiUrl = "http://localhost:" + wireMockRule.port();
        String userToken = "USER_TOKEN";
        String consumerToken = "CONSUMER_TOKEN";

        givenThat(post(urlEqualTo("/graphql"))
                .withHeader(ACCEPT, equalTo(MEDIA_TYPE_JSON.toString()))
                .withHeader("Authorization", equalTo("Bearer " + userToken))
                .withHeader("Nav-Consumer-Token", equalTo("Bearer " + consumerToken))
                .withHeader("Tema", equalTo("GEN"))
                .withRequestBody(equalToJson(graphqlJsonRequest))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(graphqlJsonResponse))
        );

        PdlClientImpl pdlClient = new PdlClientImpl(apiUrl, Tema.GEN, () -> userToken, () -> consumerToken);

        String jsonResponse = pdlClient.rawRequest(graphqlJsonRequest);

        assertEquals(graphqlJsonResponse, jsonResponse);
    }


}
