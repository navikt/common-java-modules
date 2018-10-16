package no.nav.brukerdialog.security.oidc;

import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.sbl.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static no.nav.brukerdialog.security.oidc.TokenProviderUtil.basicCredentials;
import static no.nav.brukerdialog.security.oidc.TokenProviderUtil.findToken;

public class IdTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(IdTokenProvider.class);
    private final Client client = RestUtils.createClient();
    private String issoHostUrl;
    private String issoRpUserPassword;

    public IdTokenProvider() {
        this(IdTokenProviderConfig.resolveFromSystemProperties());
    }

    public IdTokenProvider(IdTokenProviderConfig idTokenProviderConfig) {
        this.issoHostUrl = idTokenProviderConfig.issoHostUrl;
        this.issoRpUserPassword = idTokenProviderConfig.issoRpUserPassword;
    }

    public OidcCredential getToken(String refreshToken, String openamClient) {
        return TokenProviderUtil.getToken(() -> createTokenRequest(refreshToken, openamClient, client), this::extractToken);
    }

    protected Response createTokenRequest(String refreshToken, String openamClient, Client client) {
        String realm = "/";
        String data = "grant_type=refresh_token"
                + "&scope=openid"
                + "&realm=" + realm
                + "&refresh_token=" + refreshToken;
        log.debug("Refreshing ID-token by POST to " + issoHostUrl);
        return client.target(issoHostUrl + "/access_token")
                .request()
                .header(AUTHORIZATION, basicCredentials(openamClient, issoRpUserPassword))
                .header(CACHE_CONTROL,"no-cache")
                .post(entity(data, APPLICATION_FORM_URLENCODED))
                ;
    }

    private OidcCredential extractToken(String responseString) {
        return new OidcCredential(findToken(responseString, "id_token"));
    }

}
