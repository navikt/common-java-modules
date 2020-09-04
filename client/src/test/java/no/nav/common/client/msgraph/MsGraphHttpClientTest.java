package no.nav.common.client.msgraph;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.client.TestUtils;
import no.nav.common.json.JsonUtils;
import org.junit.Rule;
import org.junit.Test;

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
    public void skal_pinge_riktig_url() {
        String baseUrl = "http://localhost:" + wireMockRule.port();
        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        MsGraphHttpClient client = new MsGraphHttpClient(baseUrl);

        assertTrue(client.checkHealth().isHealthy());
        verify(getRequestedFor(urlEqualTo("/")));
    }

}
