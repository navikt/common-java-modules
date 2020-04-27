package no.nav.common.abac;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.abac.domain.request.XacmlRequest;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.common.abac.TestUtils.getContentFromJsonFile;
import static org.junit.Assert.assertEquals;

public class AbacHttpClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void sendRequest__skal_lage_riktig_request() {
        String endpointUrl = "http://localhost:" + wireMockRule.port();
        stubFor(post(urlEqualTo("/")));

        AbacHttpClient client = new AbacHttpClient(endpointUrl, "username", "password");

        XacmlRequest request = MockXacmlRequest.getXacmlRequest();
        String xacmlJson = XacmlMapper.mapRequestToEntity(request);

        client.sendRequest(xacmlJson);

        verify(postRequestedFor(urlEqualTo("/"))
                .withHeader("Content-Type", equalTo("application/xacml+json; charset=utf-8"))
                .withBasicAuth(new BasicCredentials("username", "password"))
        );
    }

    @Test
    public void sendRequest__skal_returnere_respons() {
        String endpointUrl = "http://localhost:" + wireMockRule.port();
        String responseJson = getContentFromJsonFile("xacmlresponse-simple.json");
        stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/xacml+json")
                        .withBody(responseJson)));

        AbacHttpClient client = new AbacHttpClient(endpointUrl, "username", "password");

        XacmlRequest request = MockXacmlRequest.getXacmlRequest();
        String xacmlJson = XacmlMapper.mapRequestToEntity(request);

        assertEquals(responseJson, client.sendRequest(xacmlJson));
    }

}
