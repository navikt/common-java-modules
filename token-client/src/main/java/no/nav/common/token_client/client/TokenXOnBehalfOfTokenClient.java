package no.nav.common.token_client.client;

import static java.util.Optional.ofNullable;
import static no.nav.common.token_client.utils.TokenClientUtils.clientAssertionClaims;
import static no.nav.common.token_client.utils.TokenClientUtils.clientAssertionHeader;
import static no.nav.common.token_client.utils.TokenClientUtils.signedClientAssertion;
import static no.nav.common.token_client.utils.TokenUtils.getSubject;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.TokenTypeURI;
import com.nimbusds.oauth2.sdk.tokenexchange.TokenExchangeGrant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import no.nav.common.token_client.cache.TokenCache;

public class TokenXOnBehalfOfTokenClient extends AbstractTokenClient implements OnBehalfOfTokenClient {

    public TokenXOnBehalfOfTokenClient(
            String clientId, String tokenEndpointUrl, String privateJwk, TokenCache tokenCache) {
        super(clientId, tokenEndpointUrl, privateJwk, tokenCache);
    }

    @Override
    public String exchangeOnBehalfOfToken(String tokenScope, String accessToken) {
        String cacheKey = tokenScope + "-" + getSubject(accessToken);

        return ofNullable(tokenCache)
                .map(cache -> cache.getFromCacheOrTryProvider(cacheKey, () -> exchangeToken(tokenScope, accessToken)))
                .orElseGet(() -> exchangeToken(tokenScope, accessToken));
    }

    @SneakyThrows
    private String exchangeToken(String tokenScope, String accessToken) {
        PrivateKeyJWT signedJwt = signedClientAssertion(
                clientAssertionHeader(privateJwkKeyId),
                clientAssertionClaims(clientId, tokenEndpoint.toString()),
                assertionSigner);

        TokenRequest request = new TokenRequest(
                tokenEndpoint,
                signedJwt,
                new TokenExchangeGrant(new BearerAccessToken(accessToken), TokenTypeURI.ACCESS_TOKEN),
                new Scope(tokenScope),
                null,
                additionalClaims(tokenScope, accessToken));

        TokenResponse response = parseTokenResponse(request.toHTTPRequest().send(), "TokenX OBO token request");

        AccessTokenResponse successResponse = response.toSuccessResponse();

        return successResponse.getTokens().getAccessToken().getValue();
    }

    private static Map<String, List<String>> additionalClaims(String audience, String accessToken) {
        Map<String, List<String>> customParams = new HashMap<>();
        customParams.put("audience", List.of(audience));
        customParams.put("subject_token", List.of(accessToken));
        customParams.put("subject_token_type", List.of("urn:ietf:params:oauth:token-type:jwt"));

        return customParams;
    }
}
