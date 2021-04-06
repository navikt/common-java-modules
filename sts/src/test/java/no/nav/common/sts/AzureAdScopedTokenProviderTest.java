package no.nav.common.sts;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.String.format;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.junit.Assert.assertEquals;

public class AzureAdScopedTokenProviderTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void should_create_correct_token_request() {
        String clientId = "CLIENT_ID";
        String clientSecret = "CLIENT_SECRET";
        String scope = "SCOPE";
        String accessToken = new PlainJWT(
                new JWTClaimsSet.Builder().expirationTime(new Date(System.currentTimeMillis() + 10_000)).build()
        ).serialize();

        String baseUrl = "http://localhost:" + wireMockRule.port();
        String tokenUrl = "/oauth2/v2.0/token";
        String json = format("{ \"token_type\": \"bearer\", \"access_token\": \"%s\" }", accessToken);

        givenThat(post(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .withBody(json))
        );

        AzureAdScopedTokenProvider tokenProvider = new AzureAdScopedTokenProvider(clientId, clientSecret, joinPaths(baseUrl, tokenUrl));
        String token = tokenProvider.getToken(scope);

        assertEquals(accessToken, token);

        verify(postRequestedFor(urlEqualTo(tokenUrl))
                .withRequestBody(containing("grant_type=client_credentials"))
                .withRequestBody(containing("scope=" + scope))
                .withBasicAuth(new BasicCredentials(clientId, clientSecret))
        );
    }

}
