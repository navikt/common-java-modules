package no.nav.common.sts;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static java.lang.String.format;
import static no.nav.common.test.wiremock.WireMockTestUtils.createWireMockExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import no.nav.common.rest.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NaisSystemUserTokenProviderTest {

    private static final String ACCESS_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @RegisterExtension
    public WireMockExtension wireMockRule = createWireMockExtension();

    @Test
    public void skal_hente_token_fra_sts() {
        String baseUrl = "http://localhost:" + wireMockRule.getPort();
        String json = format("{ \"access_token\": \"%s\", \"expires\": 3600 }", ACCESS_TOKEN);

        wireMockRule.stubFor(get("/?grant_type=client_credentials&scope=openid")
                .willReturn(aResponse().withStatus(200).withBody(json)));

        NaisSystemUserTokenProvider provider =
                new NaisSystemUserTokenProvider(baseUrl, "username", "password", RestClient.baseClient());
        assertEquals(ACCESS_TOKEN, provider.getSystemUserToken());
    }
}
