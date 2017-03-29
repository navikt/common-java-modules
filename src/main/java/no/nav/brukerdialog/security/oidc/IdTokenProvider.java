package no.nav.brukerdialog.security.oidc;

import no.nav.brukerdialog.security.domain.OidcCredential;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(IdTokenProvider.class);

    public OidcCredential getToken(String refreshToken) {
        return TokenProviderUtil.getToken(() -> createTokenRequest(refreshToken), s -> extractToken(s));
    }

    protected HttpUriRequest createTokenRequest(String refreshToken) {
        String host = System.getProperty("isso-host.url");
        String realm = "/";
        String username = System.getProperty("isso-rp-user.username");
        String password = System.getProperty("isso-rp-user.password");

        HttpPost request = new HttpPost(host + "/access_token");
        request.setHeader("Authorization", TokenProviderUtil.basicCredentials(username, password));
        request.setHeader("Cache-Control", "no-cache");
        request.setHeader("Content-type", "application/x-www-form-urlencoded");
        String data = "grant_type=refresh_token"
                + "&scope=openid"
                + "&realm=" + realm
                + "&refresh_token=" + refreshToken;
        log.debug("Refreshing ID-token by POST to " + host);
        request.setEntity(new StringEntity(data, "UTF-8"));
        return request;
    }

    private OidcCredential extractToken(String responseString) {
        return new OidcCredential(TokenProviderUtil.findToken(responseString, "id_token"));
    }

}
