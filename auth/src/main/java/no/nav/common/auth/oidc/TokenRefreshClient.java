package no.nav.common.auth.oidc;

import lombok.SneakyThrows;
import no.nav.common.rest.RestClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.common.rest.RestUtils.*;

public class TokenRefreshClient {

    private final static Logger log = LoggerFactory.getLogger(TokenRefreshClient.class);

    private final OkHttpClient client;

    public TokenRefreshClient() {
        this(RestClient.baseClient());
    }

    public TokenRefreshClient(OkHttpClient client) {
        this.client = client;
    }

    @SneakyThrows
    public String refreshIdToken(String refreshUrl, String refreshToken) {
        Request request = new Request.Builder()
                .url(refreshUrl)
                .post(toJsonRequestBody(new RefreshIdTokenRequest(refreshToken)))
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (response.code() >= 300) {
                String responseStr = getBodyStr(response.body()).orElse("");
                throw new RuntimeException(
                        String.format("Received unexpected status %d from %s when refreshing id token. Response: %s",
                                response.code(), refreshUrl, responseStr)
                );
            }

            return parseJsonResponseBodyOrThrow(response.body(), RefreshIdTokenResponse.class).idToken;
        } catch (Exception e) {
            log.error("Failed to refresh token with URL " + refreshUrl, e);
            throw e;
        }
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
