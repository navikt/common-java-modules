package no.nav.common.oidc.utils;

import no.nav.sbl.rest.RestUtils;

import javax.ws.rs.core.Response;
import java.util.Optional;

import static javax.ws.rs.client.Entity.json;

public class TokenRefresher {

    public static Optional<String> refreshIdToken(String refreshUrl, String refreshToken) {
       try {
           Response response = RestUtils.createClient()
                   .target(refreshUrl)
                   .request()
                   .post(json(new RefreshIdTokenDTO(refreshToken)));

           RefreshIdTokenResult refreshResult = response.readEntity(RefreshIdTokenResult.class);

           return Optional.of(refreshResult.idToken);
       } catch (Exception e) {
           return Optional.empty();
       }
    }

    public static class RefreshIdTokenDTO {
        public String refreshToken;

        public RefreshIdTokenDTO(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static class RefreshIdTokenResult {
        public String idToken;
    }

}
