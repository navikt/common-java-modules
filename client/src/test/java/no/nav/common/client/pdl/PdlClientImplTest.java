package no.nav.common.client.pdl;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.Value;
import no.nav.common.client.TestUtils;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static org.junit.Assert.assertEquals;

public class PdlClientImplTest {

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

    @Test
    public void request__skal_parse_request() {
        String uncleanedGraphqlQuery = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "pdl-client-graphql-request.graphql");
        String graphqlQuery = TestUtils.removeWhitespace(uncleanedGraphqlQuery);

        String uncleanedGraphqlJsonRequest = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "pdl-client-graphql-request.json");
        String graphqlJsonRequest = TestUtils.removeWhitespace(uncleanedGraphqlJsonRequest);

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

        GraphqlRequest<HentIdentVariables> request = new GraphqlRequest<>(graphqlQuery, new HentIdentVariables("123"));

        HentIdenterResponse graphqlResponse = pdlClient.request(request, HentIdenterResponse.class);

        String fetchedIdent = graphqlResponse.getData().hentIdenter.identer.stream().findFirst().orElseThrow().ident;

        assertEquals("8974923", fetchedIdent);
    }

    @Value
    private static class HentIdentVariables {
        String ident;
    }

    private static class HentIdenterResponse extends GraphqlResponse<HentIdenterResponse.HentIdenterResponseData> {

        private static class HentIdenterResponseData {
            HentIdenterResponse.HentIdenterResponseData.IdenterResponseData hentIdenter;

            private static class IdenterResponseData {
                List<HentIdenterResponse.HentIdenterResponseData.IdenterResponseData.IdentData> identer;

                private static class IdentData {
                    String ident;
                }
            }
        }

    }

}
