package no.nav.common.client.msgraph;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.List;
import no.nav.common.client.TestUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AzureObjectId;
import no.nav.common.types.identer.EnhetId;
import org.junit.Rule;
import org.junit.Test;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class MsGraphHttpClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/msgraph/";

    @Test
    public void hentUserData_skal_hente_data() throws Exception {
        // Arrange
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-data.json");
        UserData expectedData = JsonUtils.fromJson(json, UserData.class);

        String baseUrl = "http://localhost:" + wireMockRule.port();

        wireMockRule.stubFor(get(urlPathEqualTo("/me"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id"))
                .willReturn(aResponse().withStatus(200).withBody(json)));

        var klient = new MsGraphHttpClient(baseUrl);

        // Act
        UserData result = klient.hentUserData("ACCESS_TOKEN");

        // Assert
        assertEquals(expectedData, result);
    }

    @Test
    public void hentOnPremisesSamAccountName_skal_hente_OnPremisesSamAccountName() throws Exception {
        // Arrange
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "on-prem-sam-account-name.json");
        OnPremisesSamAccountName expectedData = JsonUtils.fromJson(json, OnPremisesSamAccountName.class);

        String baseUrl = "http://localhost:" + wireMockRule.port();

        wireMockRule.stubFor(get(urlPathEqualTo("/me"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .withQueryParam("$select", equalTo("onPremisesSamAccountName"))
                .willReturn(aResponse().withStatus(200).withBody(json)));

        var klient = new MsGraphHttpClient(baseUrl);

        // Act
        String result = klient.hentOnPremisesSamAccountName("ACCESS_TOKEN");

        // Assert
        assertEquals(expectedData.onPremisesSamAccountName, result);
    }

    @Test
    public void hentUserDataForGroup_skal_hente_data_for_gruppe() throws Exception {
        // Arrange
        var mapper = new ObjectMapper();
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-data-for-group.json");
        JsonNode root = mapper.readTree(json);
        List<UserData> expectedData = mapper.readValue(
                root.get("value").toString(),
                mapper.getTypeFactory().constructCollectionType(List.class, UserData.class));

        String baseUrl = "http://localhost:" + wireMockRule.port();

        wireMockRule.stubFor(get(urlPathEqualTo("/groups/1234/members"))
                .withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(json)));

        var klient = new MsGraphHttpClient(baseUrl);

        // Act
        List<UserData> result = klient.hentUserDataForGroup("ACCESS_TOKEN", "1234");

        // Assert
        assertEquals(expectedData, result);
    }

    @Test
    public void hentUserDataForGroup_skal_hente_data_for_enhet() throws Exception {
        // Arrange
        var mapper = new ObjectMapper();
        String groupIdJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "group-id.json");
        String userDataForGroupJson =
                TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-data-for-group.json");
        JsonNode root = mapper.readTree(userDataForGroupJson);
        List<UserData> expectedData = mapper.readValue(
                root.get("value").toString(),
                mapper.getTypeFactory().constructCollectionType(List.class, UserData.class));
        var enhetId = new EnhetId("0123");

        String baseUrl = "http://localhost:" + wireMockRule.port();

        wireMockRule.stubFor(get(urlPathEqualTo("/groups"))
                .withQueryParam("$select", equalTo("id"))
                .withQueryParam("$filter", equalTo("displayName eq '0000-GA-ENHET_0123'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(groupIdJson)));

        wireMockRule.stubFor(get(urlPathEqualTo("/groups/group-123-id/members"))
                .withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(userDataForGroupJson)));

        var klient = new MsGraphHttpClient(baseUrl);

        // Act
        List<UserData> result = klient.hentUserDataForGroup("ACCESS_TOKEN", enhetId);

        // Assert
        assertEquals(expectedData, result);

        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/groups"))
                .withQueryParam("$filter", equalTo("displayName eq '0000-GA-ENHET_0123'")));
        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/groups/group-123-id/members")));
    }

    @Test(expected = StreamReadException.class)
    public void hentUserDataForGroup_skal_kaste_exception_ved_ugyldig_json() {
        // Arrange
        String baseUrl = "http://localhost:" + wireMockRule.port();

        wireMockRule.stubFor(get(urlPathEqualTo("/groups/1234/members"))
                .withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody("{invalid json content}")));

        var klient = new MsGraphHttpClient(baseUrl);

        // Act & Assert
        klient.hentUserDataForGroup("ACCESS_TOKEN", "1234");
    }

    @Test
    public void hentUserDataForGroup_skal_kaste_exception_naar_response_body_er_tom() {
        // Arrange
        String baseUrl = "http://localhost:" + wireMockRule.port();

        wireMockRule.stubFor(get(urlPathEqualTo("/groups/1234/members"))
                .willReturn(aResponse().withStatus(200).withBody("")));

        var klient = new MsGraphHttpClient(baseUrl);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> klient.hentUserDataForGroup("ACCESS_TOKEN", "1234"));
    }

    @Test
    public void skal_pinge_riktig_url() {
        // Arrange
        String baseUrl = "http://localhost:" + wireMockRule.port();
        wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        var client = new MsGraphHttpClient(baseUrl);

        // Act
        boolean healthy = client.checkHealth().isHealthy();

        // Assert
        assertTrue(healthy);
        wireMockRule.verify(getRequestedFor(urlEqualTo("/")));
    }

    @Test
    public void hentAzureGroupId_skal_hente_id_for_enhet() throws Exception {
        // Arrange
        String groupIdJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "group-id.json");

        String baseUrl = "http://localhost:" + wireMockRule.port();
        var enhetId = new EnhetId("0123");

        wireMockRule.stubFor(get(urlPathEqualTo("/groups"))
                .withQueryParam("$select", equalTo("id"))
                .withQueryParam("$filter", equalTo("displayName eq '0000-GA-ENHET_0123'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(groupIdJson)));

        var client = new MsGraphHttpClient(baseUrl);

        // Act
        String result = client.hentAzureGroupId("ACCESS_TOKEN", enhetId);

        // Assert
        assertEquals("group-123-id", result);
    }

    @Test
    public void hentAzureIdMedNavIdent_skal_hente_id_for_nav_ident() throws Exception {
        // Arrange
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-id-response.json");

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String navIdent = "A123456";

        wireMockRule.stubFor(get(urlPathEqualTo("/users"))
                .withQueryParam("$select", equalTo("id"))
                .withQueryParam("$filter", equalTo("onPremisesSamAccountName eq 'A123456'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(json)));

        var client = new MsGraphHttpClient(baseUrl);

        // Act
        String result = client.hentAzureIdMedNavIdent("ACCESS_TOKEN", navIdent);

        // Assert
        assertEquals("azure-user-id-123", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hentAzureIdMedNavIdent_skal_kaste_exception_naar_bruker_ikke_finnes() {
        // Arrange
        String json = "{\"value\": []}";

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String navIdent = "FINNES_IKKE";

        wireMockRule.stubFor(get(urlPathEqualTo("/users"))
                .withQueryParam("$filter", equalTo("onPremisesSamAccountName eq 'FINNES_IKKE'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(json)));

        var client = new MsGraphHttpClient(baseUrl);

        // Act & Assert
        client.hentAzureIdMedNavIdent("ACCESS_TOKEN", navIdent);
    }

    @Test
    public void hentAzureIdMedNavIdent_skal_returnere_foerste_bruker_naar_flere_finnes() {
        // Arrange
        String json = "{\"value\": [{\"id\": \"first-user-id\"}, {\"id\": \"second-user-id\"}]}";

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String navIdent = "DUPLICATE";

        wireMockRule.stubFor(get(urlPathEqualTo("/users"))
                .withQueryParam("$filter", equalTo("onPremisesSamAccountName eq 'DUPLICATE'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(json)));

        var client = new MsGraphHttpClient(baseUrl);

        // Act
        String result = client.hentAzureIdMedNavIdent("ACCESS_TOKEN", navIdent);

        // Assert
        assertEquals("first-user-id", result);
    }

    @Test
    public void hentAdGroupsForUser_skal_hente_grupper() throws Exception {
        // Arrange
        var mapper = new ObjectMapper();
        String userIdJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-id-response.json");
        String adGroupsJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "ad-groups-response.json");
        JsonNode root = mapper.readTree(adGroupsJson);
        List<AdGroupData> expectedGroups = mapper.readValue(
                root.get("value").toString(),
                mapper.getTypeFactory().constructCollectionType(List.class, AdGroupData.class));

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String navIdent = "A123456";
        String azureId = "azure-user-id-123";

        // Mock the first call to get Azure ID from NAV ident
        wireMockRule.stubFor(get(urlPathEqualTo("/users"))
                .withQueryParam("$select", equalTo("id"))
                .withQueryParam("$filter", equalTo("onPremisesSamAccountName eq '" + navIdent + "'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(userIdJson)));

        // Mock the second call to get AD groups
        wireMockRule.stubFor(get(urlPathEqualTo("/users/" + azureId + "/memberOf"))
                .withQueryParam("$select", equalTo("id,displayName"))
                .withQueryParam("$top", equalTo("999"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(adGroupsJson)));

        var client = new MsGraphHttpClient(baseUrl);

        // Act
        List<AdGroupData> result = client.hentAdGroupsForUser("ACCESS_TOKEN", navIdent);

        // Assert
        assertEquals(expectedGroups, result);
        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/users/" + azureId + "/memberOf")));
    }

    @Test
    public void hentAdGroupsForUser_med_enhet_filter_skal_hente_grupper_med_enhet_prefiks() throws Exception {
        // Arrange
        String userIdJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-id-response.json");
        String adGroupsJson = TestUtils.readTestResourceFile(
                TEST_RESOURCE_BASE_PATH + "ad-groups-filtered-by-enhet-prefix-response.json");

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String navIdent = "A123456";
        String azureId = "azure-user-id-123";

        wireMockRule.stubFor(get(urlPathEqualTo("/users"))
                .withQueryParam("$select", equalTo("id"))
                .withQueryParam("$filter", equalTo("onPremisesSamAccountName eq '" + navIdent + "'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(userIdJson)));

        wireMockRule.stubFor(get(urlPathEqualTo("/users/" + azureId + "/memberOf"))
                .withQueryParam("$select", equalTo("id,displayName"))
                .withQueryParam("$top", equalTo("999"))
                .withQueryParam("$filter", equalTo("startswith(displayName,'0000-GA-ENHET_')"))
                .withHeader("ConsistencyLevel", equalTo("eventual"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(adGroupsJson)));

        MsGraphClient client = new MsGraphHttpClient(baseUrl);

        List<AdGroupData> expectedResponse = List.of(
                new AdGroupData(new AzureObjectId("1c249230-c0ec-4a7f-9ac8-f1b265034316"), ("0000-GA-ENHET_0314")),
                new AdGroupData(new AzureObjectId("239fac66-de2d-4b92-abe9-bae413d50450"), ("0000-GA-ENHET_0220")));

        // Act
        List<AdGroupData> actualResponse = client.hentAdGroupsForUser("ACCESS_TOKEN", navIdent, AdGroupFilter.ENHET);

        // Assert
        assertEquals(expectedResponse, actualResponse);
        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/users/" + azureId + "/memberOf")));
    }

    @Test
    public void hentAdGroupsForUser_med_enhet_filter_skal_hente_grupper_med_enhet_prefiks_me() throws Exception {
        // Arrange
        String adGroupsJson = TestUtils.readTestResourceFile(
                TEST_RESOURCE_BASE_PATH + "ad-groups-filtered-by-enhet-prefix-response.json");

        String baseUrl = "http://localhost:" + wireMockRule.port();

        wireMockRule.stubFor(get(urlPathEqualTo("/me/memberOf"))
                .withQueryParam("$select", equalTo("id,displayName"))
                .withQueryParam("$top", equalTo("999"))
                .withQueryParam("$filter", equalTo("startswith(displayName,'0000-GA-ENHET_')"))
                .withHeader("ConsistencyLevel", equalTo("eventual"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(adGroupsJson)));

        MsGraphClient client = new MsGraphHttpClient(baseUrl);

        List<AdGroupData> expectedResponse = List.of(
                new AdGroupData(new AzureObjectId("1c249230-c0ec-4a7f-9ac8-f1b265034316"), ("0000-GA-ENHET_0314")),
                new AdGroupData(new AzureObjectId("239fac66-de2d-4b92-abe9-bae413d50450"), ("0000-GA-ENHET_0220")));

        // Act
        List<AdGroupData> actualResponse = client.hentAdGroupsForUser("ACCESS_TOKEN", AdGroupFilter.ENHET);

        // Assert
        assertEquals(expectedResponse, actualResponse);
        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/me/memberOf")));
    }

    @Test
    public void hentAdGroupsForUser_med_tema_filter_skal_hente_grupper_med_tema_prefiks() throws Exception {
        // Arrange
        String userIdJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-id-response.json");
        String adGroupsJson = TestUtils.readTestResourceFile(
                TEST_RESOURCE_BASE_PATH + "ad-groups-filtered-by-tema-prefix-response.json");

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String navIdent = "A123456";
        String azureId = "azure-user-id-123";

        wireMockRule.stubFor(get(urlPathEqualTo("/users"))
                .withQueryParam("$select", equalTo("id"))
                .withQueryParam("$filter", equalTo("onPremisesSamAccountName eq '" + navIdent + "'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(userIdJson)));

        wireMockRule.stubFor(get(urlPathEqualTo("/users/" + azureId + "/memberOf"))
                .withQueryParam("$select", equalTo("id,displayName"))
                .withQueryParam("$top", equalTo("999"))
                .withQueryParam("$filter", equalTo("startswith(displayName,'0000-GA-TEMA_')"))
                .withHeader("ConsistencyLevel", equalTo("eventual"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(adGroupsJson)));

        MsGraphClient client = new MsGraphHttpClient(baseUrl);

        List<AdGroupData> expectedResponse = List.of(
                new AdGroupData(new AzureObjectId("248c095b-6780-464c-820d-2d7b2ea01ae9"), ("0000-GA-TEMA_PEN")),
                new AdGroupData(new AzureObjectId("3b90589a-a04a-43a7-a863-7b9c4983b7cf"), ("0000-GA-TEMA_OKO")),
                new AdGroupData(new AzureObjectId("73e61cf7-ffa3-4cdf-996b-bc48f94d9be0"), ("0000-GA-TEMA_UFM")));

        // Act
        List<AdGroupData> actualResponse = client.hentAdGroupsForUser("ACCESS_TOKEN", navIdent, AdGroupFilter.TEMA);

        // Assert
        assertEquals(expectedResponse, actualResponse);
        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/users/" + azureId + "/memberOf")));
    }
}
