package no.nav.brukerdialog.security.oidc;

import no.nav.brukerdialog.security.domain.OidcCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

public class IdTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(IdTokenProvider.class);

    public OidcCredential getToken(String refreshToken, String openamClient) {
        return TokenProviderUtil.getToken((client) -> createTokenRequest(refreshToken, openamClient, client), this::extractToken);
    }

    protected Response createTokenRequest(String refreshToken, String openamClient, Client client) {
        String host = System.getProperty("isso-host.url");
        String realm = "/";
        String password = System.getProperty("isso-rp-user.password");
        String data = "grant_type=refresh_token"
                + "&scope=openid"
                + "&realm=" + realm
                + "&refresh_token=" + refreshToken;
        log.debug("Refreshing ID-token by POST to " + host);
        return client.target(host + "/access_token")
                .request()
                .header(AUTHORIZATION, TokenProviderUtil.basicCredentials(openamClient, password))
                .header(CACHE_CONTROL,"no-cache")
                .post(entity(data, APPLICATION_FORM_URLENCODED))
                ;
    }

    private OidcCredential extractToken(String responseString) {
        return new OidcCredential(TokenProviderUtil.findToken(responseString, "id_token"));
    }

}
