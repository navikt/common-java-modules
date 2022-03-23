package no.nav.common.token_client.client;

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

@Slf4j
public class AzureAdMachineToMachineTokenClient extends AbstractTokenClient implements MachineToMachineTokenClient {

    public AzureAdMachineToMachineTokenClient(String clientId, String tokenEndpointUrl, String privateJwk, TokenCache tokenCache) {
        super(clientId, tokenEndpointUrl, privateJwk, tokenCache);
    }

    @Override
    public String createMachineToMachineToken(String tokenScope) {
        return ofNullable(tokenCache)
                .map(cache -> cache.getFromCacheOrTryProvider(tokenScope, () -> createToken(tokenScope)))
                .orElse(createToken(tokenScope));
    }

    @SneakyThrows
    private String createToken(String tokenScope) {
        PrivateKeyJWT signedJwt = signedClientAssertion(
                clientAssertionHeader(privateJwkKeyId),
                clientAssertionClaims(clientId, tokenEndpoint.toString()),
                assertionSigner
        );

        TokenRequest request = new TokenRequest(
                tokenEndpoint,
                signedJwt,
                new ClientCredentialsGrant(),
                new Scope(tokenScope),
                null,
                additionalClaims(tokenScope)
        );

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

        if (!response.indicatesSuccess()) {
            TokenErrorResponse tokenErrorResponse = response.toErrorResponse();
            log.error("Failed to fetch AzureAD M2M token for scope={}. Error: {}", tokenScope, tokenErrorResponse.toJSONObject().toString());
            throw new RuntimeException("Failed to fetch AzureAD M2M token for scope=" + tokenScope);
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();

        return successResponse.getTokens().getAccessToken().getValue();
    }

    private static Map<String, List<String>> additionalClaims(String audience) {
        Map<String, List<String>> customParams = new HashMap<>();
        customParams.put("audience", List.of(audience));

        return customParams;
    }

}
