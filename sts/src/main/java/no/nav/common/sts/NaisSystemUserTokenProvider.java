package no.nav.common.sts;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import no.nav.common.auth.oidc.discovery.OidcDiscoveryConfiguration;
import no.nav.common.auth.oidc.discovery.OidcDiscoveryConfigurationClient;
import no.nav.common.rest.client.RestClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static no.nav.common.rest.client.RestUtils.getBodyStr;
import static no.nav.common.rest.client.RestUtils.parseJsonResponseOrThrow;
import static no.nav.common.sts.utils.StsTokenUtils.tokenNeedsRefresh;
import static no.nav.common.utils.AuthUtils.basicCredentials;

/**
 * Retrieves system user tokens from NAIS Security Token Service
 * https://github.com/navikt/security-token-service
 */
public class NaisSystemUserTokenProvider implements SystemUserTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(OidcDiscoveryConfigurationClient.class);

    private final OkHttpClient client;

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
        this.client = RestClient.baseClient();
    }

    public NaisSystemUserTokenProvider(String tokenEndpoint, String srvUsername, String srvPassword, OkHttpClient client) {
        this.tokenEndpoint = tokenEndpoint;
        this.srvUsername = srvUsername;
        this.srvPassword = srvPassword;
        this.client = client;
    }

    @Override
    public String getSystemUserToken() {
        if (tokenNeedsRefresh(accessToken)) {
            accessToken = fetchSystemUserToken();
        }

        return accessToken.getParsedString();
    }

    @SneakyThrows
    private JWT fetchSystemUserToken() {
        String targetUrl = tokenEndpoint + "?grant_type=client_credentials&scope=openid";
        String basicAuth = basicCredentials(srvUsername, srvPassword);

        Request request = new Request.Builder()
                .url(targetUrl)
                .header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
                .header(AUTHORIZATION, basicAuth)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() >= 300) {
                String responseStr = getBodyStr(response).orElse("");
                throw new RuntimeException(String.format("Received unexpected status %d when requesting access token for system user. Response: %s", response.code(), responseStr));
            }

            ClientCredentialsResponse credentialsResponse = parseJsonResponseOrThrow(response, ClientCredentialsResponse.class);
            return JWTParser.parse(credentialsResponse.accessToken);
        } catch (Exception e) {
            log.error("Failed to fetch system user token from " + targetUrl, e);
            throw e;
        }
    }

    public static class ClientCredentialsResponse {
        @JsonAlias("access_token")
        public String accessToken;
    }
}
