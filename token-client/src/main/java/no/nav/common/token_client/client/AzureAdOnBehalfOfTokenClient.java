package no.nav.common.token_client.client;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.token_client.cache.TokenCache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static no.nav.common.token_client.utils.TokenClientUtils.*;
import static no.nav.common.token_client.utils.TokenUtils.hashToken;

@Slf4j
public class AzureAdOnBehalfOfTokenClient extends AbstractTokenClient implements OnBehalfOfTokenClient {

    public AzureAdOnBehalfOfTokenClient(String clientId, String tokenEndpointUrl, String privateJwk, TokenCache tokenCache) {
        super(clientId, tokenEndpointUrl, privateJwk, tokenCache);
    }

    @Override
    public String exchangeOnBehalfOfToken(String tokenScope, String accessToken) {
        String cacheKey = tokenScope + "-" + hashToken(accessToken);

        return ofNullable(tokenCache)
                .map(cache -> cache.getFromCacheOrTryProvider(cacheKey, () -> exchangeToken(tokenScope, accessToken)))
                .orElseGet(() -> exchangeToken(tokenScope, accessToken));
    }

    @SneakyThrows
    private String exchangeToken(String tokenScope, String accessToken) {
        PrivateKeyJWT signedJwt = signedClientAssertion(
                clientAssertionHeader(privateJwkKeyId),
                clientAssertionClaims(clientId, tokenEndpoint.toString()),
                assertionSigner
        );

        TokenRequest request = new TokenRequest(
                tokenEndpoint,
                signedJwt,
                new JWTBearerGrant(SignedJWT.parse(accessToken)),
                new Scope(tokenScope),
                null,
                additionalOboClaims(tokenScope, accessToken)
        );

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

        if (!response.indicatesSuccess()) {
            TokenErrorResponse tokenErrorResponse = response.toErrorResponse();
            log.error("Failed to fetch AzureAD OBO token for scope={}. Error: {}", tokenScope, tokenErrorResponse.toJSONObject().toString());
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
