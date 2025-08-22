package no.nav.common.client.msgraph;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.client.TestUtils;
import no.nav.common.json.JsonUtils;
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

        givenThat(get(urlPathEqualTo("/me")).withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN")).withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id")).willReturn(aResponse().withStatus(200).withBody(json)));

        MsGraphHttpClient klient = new MsGraphHttpClient(baseUrl);

        assertEquals(expectedData, klient.hentUserData("ACCESS_TOKEN"));
    }

    @Test
    public void hentOnPremisesSamAccountName_skal_hente_OnPremisesSamAccountName() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "on-prem-sam-account-name.json");
        OnPremisesSamAccountName expectedData = JsonUtils.fromJson(json, OnPremisesSamAccountName.class);

        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(urlPathEqualTo("/me")).withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN")).withQueryParam("$select", equalTo("onPremisesSamAccountName")).willReturn(aResponse().withStatus(200).withBody(json)));

        MsGraphHttpClient klient = new MsGraphHttpClient(baseUrl);

        assertEquals(expectedData.onPremisesSamAccountName, klient.hentOnPremisesSamAccountName("ACCESS_TOKEN"));
    }

    @Test
    public void hentUserDataForGroup_skal_hente_data_for_gruppe() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "user-data-for-group.json");
        JsonNode root = mapper.readTree(json);
        List<UserData> expectedData = mapper.readValue(root.get("value").toString(), mapper.getTypeFactory().constructCollectionType(List.class, UserData.class));

        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(urlPathEqualTo("/groups/1234")).withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id")).withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN")).willReturn(aResponse().withStatus(200).withBody(json)));

        MsGraphHttpClient klient = new MsGraphHttpClient(baseUrl);

        assertEquals(expectedData, klient.hentUserDataForGroup("ACCESS_TOKEN", "1234"));
    }

    @Test
    public void hentUserDataForGroup_skal_kaste_exception_naar_response_body_er_null() {
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(urlPathEqualTo("/groups/1234")).withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id")).withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN")).willReturn(aResponse().withStatus(200).withBody("{}")));

        MsGraphHttpClient klient = new MsGraphHttpClient(baseUrl);

        assertThrows(IllegalStateException.class, () -> klient.hentUserDataForGroup("ACCESS_TOKEN", "1234"));

    }

    @Test(expected = JsonParseException.class)
    public void hentUserDataForGroup_skal_kaste_exception_ved_ugyldig_json() {
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(urlPathEqualTo("/groups/1234")).withQueryParam("$select", equalTo("givenName,surname,displayName,mail,onPremisesSamAccountName,id")).withHeader("Authorization", equalTo("Bearer ACCESS_TOKEN")).willReturn(aResponse().withStatus(200).withBody("{invalid json content}")));

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
        assertThrows(NullPointerException.class, () -> klient.hentUserDataForGroup("ACCESS_TOKEN", "1234"));
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
