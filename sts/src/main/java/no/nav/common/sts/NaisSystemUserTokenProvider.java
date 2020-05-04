package no.nav.common.sts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;
import no.nav.common.auth.oidc.discovery.OidcDiscoveryConfiguration;
import no.nav.common.auth.oidc.discovery.OidcDiscoveryConfigurationClient;
import no.nav.common.rest.RestUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static no.nav.common.sts.SystemUserTokenUtils.tokenNeedsRefresh;
import static no.nav.common.utils.AuthUtils.basicCredentials;

/**
 * Retrieves system user tokens from the NAIS - Security Token Service
 * https://github.com/navikt/security-token-service
 */
public class NaisSystemUserTokenProvider implements SystemUserTokenProvider {

    private final Client client;

    private final String tokenEndpoint;

    private final String srvUsername;

    private final String srvPassword;

    private JWT accessToken;

    public NaisSystemUserTokenProvider(String discoveryUrl, String srvUsername, String srvPassword) {
        OidcDiscoveryConfigurationClient client = new OidcDiscoveryConfigurationClient();
        OidcDiscoveryConfiguration configuration = client.fetchDiscoveryConfiguration(discoveryUrl);

        this.tokenEndpoint = configuration.tokenEndpoint;
        this.srvUsername = srvUsername;
        this.srvPassword = srvPassword;
        this.client = RestUtils.createClient();
    }

    public NaisSystemUserTokenProvider(String tokenEndpoint, String srvUsername, String srvPassword, Client client) {
        this.tokenEndpoint = tokenEndpoint;
        this.srvUsername = srvUsername;
        this.srvPassword = srvPassword;
        this.client = client;
    }

    @Override
    public String getSystemUserToken() {
        if(tokenNeedsRefresh(accessToken)) {
            accessToken = fetchSystemUserToken();
        }

        return accessToken.getParsedString();
    }

    @SneakyThrows(ParseException.class)
    private JWT fetchSystemUserToken() {
        String targetUrl = tokenEndpoint + "?grant_type=client_credentials&scope=openid";
        String basicAuth = basicCredentials(srvUsername, srvPassword);

        Response response = client
                .target(targetUrl)
                .request()
                .header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
                .header(AUTHORIZATION, basicAuth)
                .get();

        if (response.getStatus() >= 300) {
            String responseStr = response.readEntity(String.class);
            throw new RuntimeException(String.format("Received unexpected status %d when requesting access token for system user. Response: %s", response.getStatus(), responseStr));
        }

        ClientCredentialsResponse credentialsResponse = response.readEntity(ClientCredentialsResponse.class);
        return JWTParser.parse(credentialsResponse.accessToken);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClientCredentialsResponse {

        @JsonProperty("access_token")
        public String accessToken;

    }
}
