package no.nav.common.sts;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;
import static no.nav.common.sts.utils.StsTokenUtils.tokenNeedsRefresh;

@Slf4j
public class AzureAdServiceTokenProvider implements ServiceToServiceTokenProvider {

    private final String defaultCluster;

    private final String defaultNamespace;

    private final ClientAuthentication clientAuth;

    private final URI tokenEndpoint;

    private final Map<String, JWT> cachedTokens = new ConcurrentHashMap<>();

    @SneakyThrows
    public AzureAdServiceTokenProvider(
            String defaultCluster,
            String defaultNamespace,
            String clientId,
            String clientSecret,
            String tokenEndpointUrl
    ) {
        ClientID clientID = new ClientID(clientId);
        Secret secret = new Secret(clientSecret);

        this.defaultCluster = defaultCluster;
        this.defaultNamespace = defaultNamespace;
        this.clientAuth = new ClientSecretBasic(clientID, secret);
        this.tokenEndpoint = new URI(tokenEndpointUrl);
    }

    @Override
    public String getServiceToken(String serviceName) {
        return getServiceToken(defaultCluster, defaultNamespace, serviceName);
    }

    @Override
    public String getServiceToken(String cluster, String namespace, String serviceName) {
        String serviceIdentifier = createServiceIdentifier(cluster, namespace, serviceName);

        JWT token = cachedTokens.get(serviceIdentifier);

        if (tokenNeedsRefresh(token)) {
            token = fetchServiceToken(serviceIdentifier);
            cachedTokens.put(serviceIdentifier, token);
        }

        return token.getParsedString();
    }

    @SneakyThrows
    private JWT fetchServiceToken(String serviceIdentifier) {
        Scope scope = createScope(serviceIdentifier);
        ClientCredentialsGrant grant = new ClientCredentialsGrant();

        TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth, grant, scope);
        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

        if (!response.indicatesSuccess()) {
            TokenErrorResponse tokenErrorResponse = response.toErrorResponse();
            log.error("Failed to fetch service token for {}. Error: {}", serviceIdentifier, tokenErrorResponse.toJSONObject().toString());
            throw new RuntimeException("Failed to fetch service token for " + serviceIdentifier);
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();
        AccessToken accessToken = successResponse.getTokens().getAccessToken();

        return JWTParser.parse(accessToken.getValue());
    }

    private static Scope createScope(String serviceIdentifier) {
        return new Scope(format("api://%s/.default", serviceIdentifier));
    }

    private static String createServiceIdentifier(String cluster, String namespace, String serviceName) {
        return format("%s.%s.%s", cluster, namespace, serviceName);
    }

}
