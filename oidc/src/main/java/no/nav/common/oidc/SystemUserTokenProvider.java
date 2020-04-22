package no.nav.common.oidc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;
import no.nav.common.oidc.discovery.OidcDiscoveryConfiguration;
import no.nav.common.oidc.discovery.OidcDiscoveryConfigurationClient;
import no.nav.common.oidc.utils.TokenUtils;
import no.nav.common.rest.RestUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static no.nav.common.utils.AuthUtils.basicCredentials;

public class SystemUserTokenProvider {

    private final static int MINIMUM_TIME_TO_EXPIRE_BEFORE_REFRESH = 60 * 1000; // 1 minute

    private final Client client;

    private final String tokenEndpoint;

    private final String srvUsername;

    private final String srvPassword;

    private JWT accessToken;

    public SystemUserTokenProvider(String discoveryUrl, String srvUsername, String srvPassword) {
        OidcDiscoveryConfigurationClient client = new OidcDiscoveryConfigurationClient();
        OidcDiscoveryConfiguration configuration = client.fetchDiscoveryConfiguration(discoveryUrl);

        this.tokenEndpoint = configuration.tokenEndpoint;
        this.srvUsername = srvUsername;
        this.srvPassword = srvPassword;
        this.client = RestUtils.createClient();
    }

    public SystemUserTokenProvider(String tokenEndpoint, String srvUsername, String srvPassword, Client client) {
        this.tokenEndpoint = tokenEndpoint;
        this.srvUsername = srvUsername;
        this.srvPassword = srvPassword;
        this.client = client;
    }

    public String getSystemUserAccessToken() {
        if(tokenIsSoonExpired()) {
            refreshToken();
        }

        return accessToken.getParsedString();
    }

    @SneakyThrows(ParseException.class)
    private void refreshToken() {
        ClientCredentialsResponse clientCredentials = fetchSystemUserAccessToken();
        this.accessToken = JWTParser.parse(clientCredentials.accessToken);
    }

    private boolean tokenIsSoonExpired() {
        return accessToken == null || TokenUtils.expiresWithin(accessToken, MINIMUM_TIME_TO_EXPIRE_BEFORE_REFRESH);
    }

    private ClientCredentialsResponse fetchSystemUserAccessToken() {
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

        return response.readEntity(ClientCredentialsResponse.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClientCredentialsResponse {

        @JsonProperty("access_token")
        public String accessToken;

    }
}
