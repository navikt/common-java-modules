package no.nav.modig.security.filter;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link OpenAMService}
 */
public class OpenAMServiceTest {
    private String tokenValidUri = "/openam/identity/json/isTokenValid";
    private String contentType = "Content-Type";
    private String json = "application/json";

    private OpenAMService service;

    protected WireMockConfiguration wireMockConfig = wireMockConfig().port(8000);// No-args constructor defaults to port 8080

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig);


    @Before
    public void setUp() throws Exception {
        System.setProperty("openam.restUrl", "http://localhost:8000" + tokenValidUri);
        service = new OpenAMService();
    }

    @Test
    public void returnsTrueWhenTokenOk() {
        stubFor(get(urlMatching(tokenValidUri + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(200)
                        .withBody("{\"boolean\":true}")));

        boolean tokenValid = service.isTokenValid("validtoken");
        assertThat(tokenValid, is(true));
    }



    @Test
    public void returnsFalseWhenStatusIsNotOK() {
        stubFor(get(urlMatching(tokenValidUri + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(401)
                        .withBody("{\"exception\":{\"message\":\"Invalid session ID.AQIC5wM2LY4SfcyJGDdeM4jAwUXux7RBOEymzzLm08mMxoI.*AAJTSQACMDIAAlMxAAIwMQ..*\",\"name\":\"com.sun.identity.idsvcs.InvalidToken\"}}")));

        boolean tokenValid = service.isTokenValid("validtoken");
        assertThat(tokenValid, is(false));
    }


    @Test
    public void returnsFalseWhenTokenOk() {
        stubFor(get(urlMatching(tokenValidUri + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(401)
                        .withBody("{\"exception\":{\"message\":\"Invalid session ID.AQIC5wM2LY4SfcyJGDdeM4jAwUXux7RBOEymzzLm08mMxoI.*AAJTSQACMDIAAlMxAAIwMQ..*\",\"name\":\"com.sun.identity.idsvcs.InvalidToken\"}}")));

        boolean tokenValid = service.isTokenValid("validtoken");
        assertThat(tokenValid, is(false));
    }

    @Test(expected = RuntimeException.class)
    public void throwsExceptionIfOpenAmServiceIsDown() {
        stubFor(get(urlMatching(tokenValidUri + ".*"))
                .willReturn(aResponse()
                        .withHeader(contentType, json)
                        .withStatus(500)
                        .withBody("500 unavailable")));


        service.isTokenValid("validtoken");
    }
}
