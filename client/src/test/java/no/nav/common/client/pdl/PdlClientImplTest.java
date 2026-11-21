package no.nav.common.client.pdl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;

import java.util.List;

import lombok.Value;
import no.nav.common.client.TestUtils;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.common.json.JsonUtils;
import org.junit.Rule;
import org.junit.Test;

public class PdlClientImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/pdl/client/";

    @Test
    public void rawRequest__skal_lage_riktig_request_og_forwarde_respons() {
        // Arrange
        String graphqlJsonRequest = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "graphql-request.json");
        String graphqlJsonResponse = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "graphql-response.json");

        String apiUrl = "http://localhost:" + wireMockRule.port();
        String userToken = "USER_TOKEN";
        String consumerToken = "CONSUMER_TOKEN";
        String behandlingsnummer = "B123";

        givenThat(post(urlEqualTo("/graphql"))
            .withHeader(ACCEPT, equalTo(MEDIA_TYPE_JSON.toString()))
            .withHeader("Authorization", equalTo("Bearer " + userToken))
            .withHeader("Nav-Consumer-Token", equalTo("Bearer " + consumerToken))
            .withHeader("Tema", equalTo("GEN"))
            .withHeader("behandlingsnummer", equalTo(behandlingsnummer))
            .withRequestBody(equalToJson(graphqlJsonRequest))
            .willReturn(aResponse().withStatus(200).withBody(graphqlJsonResponse)));

        var pdlClient = new PdlClientImpl(apiUrl, Tema.GEN, () -> userToken, () -> consumerToken, behandlingsnummer);

        // Act
        String jsonResponse = pdlClient.rawRequest(graphqlJsonRequest);

        // Assert
        assertEquals(graphqlJsonResponse, jsonResponse);
    }

    @Test
    public void request__skal_lage_request_og_parse_response() {
        // Arrange
        String graphqlQuery =
            TestUtils.readTestResourceFileWithoutWhitespace(TEST_RESOURCE_BASE_PATH + "hent-identer-query.graphql");
        String graphqlJsonRequest =
            TestUtils.readTestResourceFileWithoutWhitespace(TEST_RESOURCE_BASE_PATH + "graphql-request.json");

        String graphqlJsonResponse = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "graphql-response.json");

        String apiUrl = "http://localhost:" + wireMockRule.port();
        String userToken = "USER_TOKEN";
        String consumerToken = "CONSUMER_TOKEN";

        givenThat(post(urlEqualTo("/graphql"))
            .withHeader(ACCEPT, equalTo(MEDIA_TYPE_JSON.toString()))
            .withHeader("Authorization", equalTo("Bearer " + userToken))
            .withHeader("Nav-Consumer-Token", equalTo("Bearer " + consumerToken))
            .withHeader("Tema", equalTo("GEN"))
            .withHeader("behandlingsnummer", equalTo(""))
            .withRequestBody(equalToJson(graphqlJsonRequest))
            .willReturn(aResponse().withStatus(200).withBody(graphqlJsonResponse)));

        var pdlClient = new PdlClientImpl(apiUrl, Tema.GEN, () -> userToken, () -> consumerToken, "");

        var request = new GraphqlRequest<>(graphqlQuery, new HentIdentVariables("123"));

        // Act
        HentIdenterResponse graphqlResponse = pdlClient.request(request, HentIdenterResponse.class);

        // Assert
        String fetchedIdent = graphqlResponse.getData().hentIdenter.identer.stream()
            .findFirst()
            .orElseThrow()
            .ident;

        assertEquals("8974923", fetchedIdent);
    }

    @Test
    public void request__skal_parse_error_response() {
        // Arrange
        String graphqlErrorJsonResponse =
            TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "graphql-response-error.json");

        String apiUrl = "http://localhost:" + wireMockRule.port();
        String userToken = "USER_TOKEN";
        String consumerToken = "CONSUMER_TOKEN";

        givenThat(post(urlEqualTo("/graphql"))
            .withHeader(ACCEPT, equalTo(MEDIA_TYPE_JSON.toString()))
            .withHeader("Authorization", equalTo("Bearer " + userToken))
            .withHeader("Nav-Consumer-Token", equalTo("Bearer " + consumerToken))
            .withHeader("Tema", equalTo("GEN"))
            .withHeader("behandlingsnummer", equalTo(""))
            .willReturn(aResponse().withStatus(200).withBody(graphqlErrorJsonResponse)));

        var pdlClient = new PdlClientImpl(apiUrl, Tema.GEN, () -> userToken, () -> consumerToken, "");

        var request = new GraphqlRequest<>("some query", new HentIdentVariables("123"));

        // Act
        HentIdenterResponse graphqlResponse = pdlClient.request(request, HentIdenterResponse.class);

        // Assert
        var isErrorEqualPattern = new EqualToJsonPattern(graphqlErrorJsonResponse, false, false);

        assertTrue(isErrorEqualPattern.match(JsonUtils.toJson(graphqlResponse)).isExactMatch());
    }

    @Value
    private static class HentIdentVariables {
        String ident;
    }

    private static class HentIdenterResponse extends GraphqlResponse<HentIdenterResponse.HentIdenterResponseData> {

        private static class HentIdenterResponseData {
            HentIdenterResponse.HentIdenterResponseData.IdenterResponseData hentIdenter;

            private static class IdenterResponseData {
                @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // Fylles av Jackson ved deserialisering.
                List<HentIdenterResponse.HentIdenterResponseData.IdenterResponseData.IdentData> identer;

                private static class IdentData {
                    String ident;
                }
            }
        }
    }
}
