package no.nav.common.sts;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

/**
 * Provides access tokens from Azure Ad through OAuth 2.0 credentials flow
 */
@Slf4j
public class AzureAdScopedTokenProvider implements ScopedTokenProvider {

    private final ClientAuthentication clientAuth;

    private final URI tokenEndpoint;

    @SneakyThrows
    public AzureAdScopedTokenProvider(String clientId, String clientSecret, String tokenEndpointUrl) {
        ClientID clientID = new ClientID(clientId);
        Secret secret = new Secret(clientSecret);

        this.clientAuth = new ClientSecretBasic(clientID, secret);
        this.tokenEndpoint = new URI(tokenEndpointUrl);
    }

    @SneakyThrows
    @Override
    public String getToken(String scope) {
        Scope requestScope = new Scope(scope);
        ClientCredentialsGrant grant = new ClientCredentialsGrant();

        TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth, grant, requestScope);
        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

        if (!response.indicatesSuccess()) {
            TokenErrorResponse tokenErrorResponse = response.toErrorResponse();
            log.error("Failed to fetch service token for {}. Error: {}", scope, tokenErrorResponse.toJSONObject().toString());
            throw new RuntimeException("Failed to fetch service token for " + scope);
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();
        AccessToken accessToken = successResponse.getTokens().getAccessToken();

        return accessToken.getValue();
    }

}
