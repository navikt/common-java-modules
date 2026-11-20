package no.nav.common.token_client.client;

import static java.util.Optional.ofNullable;
import static no.nav.common.token_client.utils.TokenClientUtils.clientAssertionClaims;
import static no.nav.common.token_client.utils.TokenClientUtils.clientAssertionHeader;
import static no.nav.common.token_client.utils.TokenClientUtils.signedClientAssertion;
import static no.nav.common.token_client.utils.TokenUtils.getSubject;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.JWTBearerGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.token_client.cache.TokenCache;

@Slf4j
public class AzureAdOnBehalfOfTokenClient extends AbstractTokenClient implements OnBehalfOfTokenClient {

    public AzureAdOnBehalfOfTokenClient(
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
                new JWTBearerGrant(SignedJWT.parse(accessToken)),
                new Scope(tokenScope),
                null,
                additionalOboClaims(tokenScope, accessToken));

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

        if (!response.indicatesSuccess()) {
            TokenErrorResponse tokenErrorResponse = response.toErrorResponse();
            log.error(
                    "Failed to fetch AzureAD OBO token for scope={}. Error: {}",
                    tokenScope,
                    tokenErrorResponse.toJSONObject().toString());
            throw new RuntimeException("Failed to fetch AzureAD OBO token for scope=" + tokenScope);
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();

        return successResponse.getTokens().getAccessToken().getValue();
    }

    private static Map<String, List<String>> additionalOboClaims(String audience, String accessToken) {
        Map<String, List<String>> customParams = new HashMap<>();
        customParams.put("audience", List.of(audience));
        customParams.put("subject_token", List.of(accessToken));
        customParams.put("requested_token_use", List.of("on_behalf_of"));
        customParams.put("subject_token_type", List.of("urn:ietf:params:oauth:token-type:jwt"));

        return customParams;
    }
}
