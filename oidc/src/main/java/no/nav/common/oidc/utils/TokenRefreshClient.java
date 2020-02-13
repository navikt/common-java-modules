package no.nav.common.oidc.utils;

import no.nav.sbl.rest.RestUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static javax.ws.rs.client.Entity.json;

public class TokenRefreshClient {

    private final Client client;

    public TokenRefreshClient() {
        this(RestUtils.createClient());
    }

    TokenRefreshClient(Client client) {
        this.client = client;
    }

    public String refreshIdToken(String refreshUrl, String refreshToken) {
        Response response = client
                .target(refreshUrl)
                .request()
                .post(json(new RefreshIdTokenRequest(refreshToken)));

        if (response.getStatus() >= 300) {
            String responseStr = response.readEntity(String.class);
            throw new RuntimeException(String.format("Received unexpected status %d from %s when refreshing id token. Response: %s", response.getStatus(), refreshUrl, responseStr));
        }

        return response.readEntity(RefreshIdTokenResponse.class).idToken;
    }

    public static class RefreshIdTokenRequest {
        public String refreshToken;

        public RefreshIdTokenRequest() {}
        public RefreshIdTokenRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static class RefreshIdTokenResponse {
        public String idToken;

        public RefreshIdTokenResponse() {}
        public RefreshIdTokenResponse(String idToken) {
            this.idToken = idToken;
        }
    }

}
