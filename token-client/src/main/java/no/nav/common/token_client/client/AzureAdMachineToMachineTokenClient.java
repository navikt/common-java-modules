package no.nav.common.token_client.client;

import static java.util.Optional.ofNullable;
import static no.nav.common.token_client.utils.TokenClientUtils.clientAssertionClaims;
import static no.nav.common.token_client.utils.TokenClientUtils.clientAssertionHeader;
import static no.nav.common.token_client.utils.TokenClientUtils.signedClientAssertion;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import no.nav.common.token_client.cache.TokenCache;

public class AzureAdMachineToMachineTokenClient extends AbstractTokenClient implements MachineToMachineTokenClient {

    public AzureAdMachineToMachineTokenClient(
            String clientId, String tokenEndpointUrl, String privateJwk, TokenCache tokenCache) {
        super(clientId, tokenEndpointUrl, privateJwk, tokenCache);
    }

    @Override
    public String createMachineToMachineToken(String tokenScope) {
        return ofNullable(tokenCache)
                .map(cache -> cache.getFromCacheOrTryProvider(tokenScope, () -> createToken(tokenScope)))
                .orElseGet(() -> createToken(tokenScope));
    }

    @SneakyThrows
    private String createToken(String tokenScope) {
        PrivateKeyJWT signedJwt = signedClientAssertion(
                clientAssertionHeader(privateJwkKeyId),
                clientAssertionClaims(clientId, tokenEndpoint.toString()),
                assertionSigner);

        TokenRequest request = new TokenRequest(
                tokenEndpoint,
                signedJwt,
                new ClientCredentialsGrant(),
                new Scope(tokenScope),
                null,
                additionalClaims(tokenScope));

        TokenResponse response = parseTokenResponse(request.toHTTPRequest().send(), "AzureAD M2M token request");

        AccessTokenResponse successResponse = response.toSuccessResponse();

        AccessToken accessToken = successResponse.getTokens().getAccessToken();

        return accessToken.getValue();
    }

    private static Map<String, List<String>> additionalClaims(String audience) {
        Map<String, List<String>> customParams = new HashMap<>();
        customParams.put("audience", List.of(audience));

        return customParams;
    }
}
