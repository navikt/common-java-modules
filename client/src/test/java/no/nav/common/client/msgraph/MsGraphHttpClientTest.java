package no.nav.common.client.msgraph;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.client.TestUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AzureObjectId;
import no.nav.common.types.identer.EnhetId;
import okhttp3.*;
import org.junit.Rule;
import org.junit.Test;
import com.fasterxml.jackson.core.JsonParseException;
import wiremock.com.fasterxml.jackson.core.JsonProcessingException;
import wiremock.com.fasterxml.jackson.databind.JsonNode;
import wiremock.com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import okhttp3.Call;

import java.lang.reflect.Field;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MsGraphHttpClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/msgraph/";

    // Helper method to set private field
    private void setField(Object target, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField("client");
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void hentUserData_skal_hente_data() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-data.json");
        UserData expectedData = JsonUtils.fromJson(json, UserData.class);

        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(urlPathEqualTo("/me"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id"))
                .willReturn(aResponse().withStatus(200).withBody(json)));

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
                .willReturn(aResponse().withStatus(200).withBody(json)));

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

        givenThat(get(urlPathEqualTo("/groups/1234/members"))
                .withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(json)));

        MsGraphHttpClient klient = new MsGraphHttpClient(baseUrl);

        assertEquals(expectedData, klient.hentUserDataForGroup("ACCESS_TOKEN", "1234"));
    }

    @Test
    public void hentUserDataForGroup_skal_hente_data_for_enhet() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String groupIdJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "group-id.json");
        String userDataForGroupJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-data-for-group.json");
        JsonNode root = mapper.readTree(userDataForGroupJson);
        List<UserData> expectedData = mapper.readValue(root.get("value").toString(),
                mapper.getTypeFactory().constructCollectionType(List.class, UserData.class));
        EnhetId enhetId = new EnhetId("0123");

        String baseUrl = "http://localhost:" + wireMockRule.port();


        givenThat(get(urlPathEqualTo("/groups"))
                .withQueryParam("$select", equalTo("id"))
                .withQueryParam("$filter", equalTo("displayName eq '0000-GA-ENHET_0123'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(groupIdJson)));

        givenThat(get(urlPathEqualTo("/groups/group-123-id/members"))
                .withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody(userDataForGroupJson)));

        MsGraphHttpClient klient = new MsGraphHttpClient(baseUrl);

        List<UserData> result = klient.hentUserDataForGroup("ACCESS_TOKEN", enhetId);

        assertEquals(expectedData, result);

        verify(getRequestedFor(urlPathEqualTo("/groups"))
                .withQueryParam("$filter", equalTo("displayName eq '0000-GA-ENHET_0123'")));
        verify(getRequestedFor(urlPathEqualTo("/groups/group-123-id/members")));

    }


    @Test(expected = JsonParseException.class)
    public void hentUserDataForGroup_skal_kaste_exception_ved_ugyldig_json() {
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(urlPathEqualTo("/groups/1234/members"))
                .withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse().withStatus(200).withBody("{invalid json content}")));

        MsGraphHttpClient klient = new MsGraphHttpClient(baseUrl);
        klient.hentUserDataForGroup("ACCESS_TOKEN", "1234");
    }

    @Test
    public void hentUserDataForGroup_skal_kaste_exception_naar_response_body_er_faktisk_null() throws Exception {
        OkHttpClient mockClient = mock(OkHttpClient.class);
        Call mockCall = mock(Call.class);
        Response mockResponse = mock(Response.class);

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(null);

        MsGraphHttpClient klient = new MsGraphHttpClient("http://localhost");
        setField(klient, mockClient);
        assertThrows(IllegalStateException.class, () -> klient.hentUserDataForGroup("ACCESS_TOKEN", "1234"));
    }

    @Test
    public void skal_pinge_riktig_url() {
        String baseUrl = "http://localhost:" + wireMockRule.port();
        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        MsGraphHttpClient client = new MsGraphHttpClient(baseUrl);

        assertTrue(client.checkHealth().isHealthy());
        verify(getRequestedFor(urlEqualTo("/")));
    }

    @Test
    public void hentAzureGroupId_skal_hente_id_for_enhet() {
        String groupIdJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "group-id.json");

        String baseUrl = "http://localhost:" + wireMockRule.port();
        EnhetId enhetId = new EnhetId("0123");

        givenThat(get(urlPathEqualTo("/groups"))
                .withQueryParam("$select", equalTo("id"))
                .withQueryParam("$filter", equalTo("displayName eq '0000-GA-ENHET_0123'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(groupIdJson)));

        MsGraphHttpClient client = new MsGraphHttpClient(baseUrl);

        String result = client.hentAzureGroupId("ACCESS_TOKEN", enhetId);

        assertEquals("group-123-id", result);
    }

    @Test
    public void hentAzureIdMedNavIdent_skal_hente_id_for_nav_ident() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-id-response.json");

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String navIdent = "A123456";

        givenThat(get(urlPathEqualTo("/users"))
                .withQueryParam("$select", equalTo("id"))
                .withQueryParam("$filter", equalTo("onPremisesSamAccountName eq 'A123456'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json)));

        MsGraphHttpClient client = new MsGraphHttpClient(baseUrl);

        String result = client.hentAzureIdMedNavIdent("ACCESS_TOKEN", navIdent);

        assertEquals("azure-user-id-123", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hentAzureIdMedNavIdent_skal_kaste_exception_naar_bruker_ikke_finnes() {
        String json = "{\"value\": []}";

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String navIdent = "FINNES_IKKE";

        givenThat(get(urlPathEqualTo("/users"))
                .withQueryParam("$filter", equalTo("onPremisesSamAccountName eq 'FINNES_IKKE'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json)));

        MsGraphHttpClient client = new MsGraphHttpClient(baseUrl);

        client.hentAzureIdMedNavIdent("ACCESS_TOKEN", navIdent);
    }

    @Test
    public void hentAzureIdMedNavIdent_skal_returnere_foerste_bruker_naar_flere_finnes() {
        String json = "{\"value\": [{\"id\": \"first-user-id\"}, {\"id\": \"second-user-id\"}]}";

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String navIdent = "DUPLICATE";

        givenThat(get(urlPathEqualTo("/users"))
                .withQueryParam("$filter", equalTo("onPremisesSamAccountName eq 'DUPLICATE'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json)));

        MsGraphHttpClient client = new MsGraphHttpClient(baseUrl);

        String result = client.hentAzureIdMedNavIdent("ACCESS_TOKEN", navIdent);

        assertEquals("first-user-id", result);
    }

    @Test
    public void hentAdGroupsForUser_skal_hente_grupper() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String userIdJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-id-response.json");
        String adGroupsJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "ad-groups-response.json");
        JsonNode root = mapper.readTree(adGroupsJson);
        List<AdGroupData> expectedGroups = mapper.readValue(root.get("value").toString(),
                mapper.getTypeFactory().constructCollectionType(List.class, AdGroupData.class));

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String navIdent = "A123456";
        String azureId = "azure-user-id-123";

        // Mock the first call to get Azure ID from NAV ident
        givenThat(get(urlPathEqualTo("/users"))
                .withQueryParam("$select", equalTo("id"))
                .withQueryParam("$filter", equalTo("onPremisesSamAccountName eq '" + navIdent + "'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(userIdJson)));

        // Mock the second call to get AD groups
        givenThat(get(urlPathEqualTo("/users/" + azureId + "/memberOf"))
                .withQueryParam("$select", equalTo("id,displayName"))
                .withQueryParam("$top", equalTo("999"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(adGroupsJson)));

        MsGraphHttpClient client = new MsGraphHttpClient(baseUrl);

        List<AdGroupData> result = client.hentAdGroupsForUser("ACCESS_TOKEN", navIdent);

        assertEquals(expectedGroups, result);
        verify(getRequestedFor(urlPathEqualTo("/users/" + azureId + "/memberOf")));
    }

    @Test
    public void hentAdGroupsForUser_med_enhet_filter_skal_hente_grupper_med_enhet_prefiks() {
        String userIdJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-id-response.json");
        String adGroupsJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "ad-groups-filtered-by-enhet-prefix-response.json");

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String navIdent = "A123456";
        String azureId = "azure-user-id-123";

        givenThat(get(urlPathEqualTo("/users"))
                .withQueryParam("$select", equalTo("id"))
                .withQueryParam("$filter", equalTo("onPremisesSamAccountName eq '" + navIdent + "'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(userIdJson)));

        givenThat(get(urlPathEqualTo("/users/" + azureId + "/memberOf"))
                .withQueryParam("$select", equalTo("id,displayName"))
                .withQueryParam("$top", equalTo("999"))
                .withQueryParam("$filter", equalTo("startswith(displayName,'0000-GA-ENHET_')"))
                .withHeader("ConsistencyLevel", equalTo("eventual"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(adGroupsJson)));

        MsGraphClient client = new MsGraphHttpClient(baseUrl);

        List<AdGroupData> expectedResponse = List.of(
                new AdGroupData(new AzureObjectId("1c249230-c0ec-4a7f-9ac8-f1b265034316"), ("0000-GA-ENHET_0314")),
                new AdGroupData(new AzureObjectId("239fac66-de2d-4b92-abe9-bae413d50450"), ("0000-GA-ENHET_0220")));
        List<AdGroupData> actualResponse = client.hentAdGroupsForUser("ACCESS_TOKEN", navIdent, AdGroupFilter.ENHET);
        assertEquals(expectedResponse, actualResponse);
        verify(getRequestedFor(urlPathEqualTo("/users/" + azureId + "/memberOf")));
    }

    @Test
    public void hentAdGroupsForUser_med_tema_filter_skal_hente_grupper_med_tema_prefiks() {
        String userIdJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-id-response.json");
        String adGroupsJson = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "ad-groups-filtered-by-tema-prefix-response.json");

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String navIdent = "A123456";
        String azureId = "azure-user-id-123";

        givenThat(get(urlPathEqualTo("/users"))
                .withQueryParam("$select", equalTo("id"))
                .withQueryParam("$filter", equalTo("onPremisesSamAccountName eq '" + navIdent + "'"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(userIdJson)));

        givenThat(get(urlPathEqualTo("/users/" + azureId + "/memberOf"))
                .withQueryParam("$select", equalTo("id,displayName"))
                .withQueryParam("$top", equalTo("999"))
                .withQueryParam("$filter", equalTo("startswith(displayName,'0000-GA-TEMA_')"))
                .withHeader("ConsistencyLevel", equalTo("eventual"))
                .withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(adGroupsJson)));

        MsGraphClient client = new MsGraphHttpClient(baseUrl);

        List<AdGroupData> expectedResponse = List.of(
                new AdGroupData(new AzureObjectId("248c095b-6780-464c-820d-2d7b2ea01ae9"), ("0000-GA-TEMA_PEN")),
                new AdGroupData(new AzureObjectId("3b90589a-a04a-43a7-a863-7b9c4983b7cf"), ("0000-GA-TEMA_OKO")),
                new AdGroupData(new AzureObjectId("73e61cf7-ffa3-4cdf-996b-bc48f94d9be0"), ("0000-GA-TEMA_UFM"))
        );
        List<AdGroupData> actualResponse = client.hentAdGroupsForUser("ACCESS_TOKEN", navIdent, AdGroupFilter.TEMA);
        assertEquals(expectedResponse, actualResponse);
        verify(getRequestedFor(urlPathEqualTo("/users/" + azureId + "/memberOf")));
    }
}
