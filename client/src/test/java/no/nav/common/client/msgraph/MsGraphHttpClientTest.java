package no.nav.common.client.msgraph;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.client.TestUtils;
import no.nav.common.json.JsonUtils;
import org.junit.Rule;
import org.junit.Test;
import wiremock.com.fasterxml.jackson.core.JsonProcessingException;
import wiremock.com.fasterxml.jackson.databind.JsonNode;
import wiremock.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MsGraphHttpClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/msgraph/";

    @Test
    public void hentUserData_skal_hente_data() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-data.json");
        UserData expectedData = JsonUtils.fromJson(json, UserData.class);

        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(urlPathEqualTo("/me"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        MsGraphHttpClient klient = new MsGraphHttpClient(baseUrl);

        assertEquals(expectedData, klient.hentUserData("ACCESS_TOKEN"));
    }

    @Test
    public void hentOnPremisesSamAccountName_skal_hente_OnPremisesSamAccountName() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "on-prem-sam-account-name.json");
        OnPremisesSamAccountName expectedData = JsonUtils.fromJson(json, OnPremisesSamAccountName.class);

        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(urlPathEqualTo("/me"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .withQueryParam("$select", equalTo("onPremisesSamAccountName"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        MsGraphHttpClient klient = new MsGraphHttpClient(baseUrl);

        assertEquals(expectedData.onPremisesSamAccountName, klient.hentOnPremisesSamAccountName("ACCESS_TOKEN"));
    }
    @Test
    public void hentUserDataForGroup_skal_hente_data_for_gruppe() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-data-for-group.json");
        JsonNode root = mapper.readTree(json);
        List<UserData> expectedData = mapper.readValue(root.get("value").toString(),
                mapper.getTypeFactory().constructCollectionType(List.class, UserData.class));

        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(urlPathEqualTo("/groups/1234"))
                .withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        MsGraphHttpClient klient = new MsGraphHttpClient(baseUrl);

        assertEquals(expectedData, klient.hentUserDataForGroup("ACCESS_TOKEN", "1234"));
    }

    @Test
    public void hentUserDataForGroup_skal_returnere_tom_liste_naar_response_body_er_null() {
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(urlPathEqualTo("/groups/1234"))
                .withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{}")));

        MsGraphHttpClient klient = new MsGraphHttpClient(baseUrl);

        List<UserData> result = klient.hentUserDataForGroup("ACCESS_TOKEN", "1234");

        assertTrue(result.isEmpty());
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void skal_pinge_riktig_url() {
        String baseUrl = "http://localhost:" + wireMockRule.port();
        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        MsGraphHttpClient client = new MsGraphHttpClient(baseUrl);

        assertTrue(client.checkHealth().isHealthy());
        verify(getRequestedFor(urlEqualTo("/")));
    }

}
