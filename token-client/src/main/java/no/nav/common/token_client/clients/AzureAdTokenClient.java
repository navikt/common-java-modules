package no.nav.common.token_client.clients;

import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.token_client.MachineToMachineTokenClient;
import no.nav.common.token_client.OnBehalfOfTokenClient;
import no.nav.common.token_client.utils.OidcDiscoveryClient;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.common.token_client.utils.TokenClientUtils.*;

@Slf4j
public class AzureAdTokenClient implements OnBehalfOfTokenClient, MachineToMachineTokenClient {

    private final String clientId;

    private final URI tokenEndpoint;

    private final String privateJwkKeyId;

    private final JWSSigner assertionSigner;

    @SneakyThrows
    public AzureAdTokenClient(String clientId, String privateJwk, String discoveryUrl) {
        this.clientId = clientId;

        RSAKey rsaKey = RSAKey.parse(privateJwk);
        privateJwkKeyId = rsaKey.getKeyID();
        assertionSigner = new RSASSASigner(rsaKey);

        OIDCProviderMetadata oidcProviderMetadata = OidcDiscoveryClient.fetchDiscoveryMetadata(discoveryUrl);
        tokenEndpoint = oidcProviderMetadata.getTokenEndpointURI();
    }

    @SneakyThrows
    @Override
    public String createMachineToMachineToken(String tokenScope) {
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
                additionalM2mClaims(tokenScope)
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

    @SneakyThrows
    @Override
    public String exchangeOnBehalfOfToken(String tokenScope, String accessToken) {
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

    private static Map<String, List<String>> additionalM2mClaims(String audience) {
        Map<String, List<String>> customParams = new HashMap<>();
        customParams.put("audience", List.of(audience));

        return customParams;
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
