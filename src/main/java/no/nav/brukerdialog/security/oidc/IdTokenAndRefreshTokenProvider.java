package no.nav.brukerdialog.security.oidc;

import lombok.Builder;
import no.nav.brukerdialog.security.domain.IdTokenAndRefreshToken;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.sbl.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static java.lang.System.getProperty;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static no.nav.brukerdialog.security.Constants.*;

public class IdTokenAndRefreshTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(IdTokenAndRefreshTokenProvider.class);

    static final String ENCODING = "UTF-8";

    public static final String ID_TOKEN = "id_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    private final Client client = RestUtils.createClient();


    private final Parameters parameters;

    public IdTokenAndRefreshTokenProvider() {
        this(Parameters.builder()
                .host(getProperty(ISSO_HOST_URL_PROPERTY_NAME))
                .username(getProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME))
                .password(getProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME))
                .build()
        );
    }

    public IdTokenAndRefreshTokenProvider(Parameters parameters) {
        this.parameters = parameters;
    }

    public IdTokenAndRefreshToken getToken(String authorizationCode, String redirectUri) {
        return TokenProviderUtil.getToken(() -> createTokenRequest(authorizationCode, redirectUri, client), this::extractToken);
    }

    Response createTokenRequest(String authorizationCode, String redirectUri, Client client) {
        String urlEncodedRedirectUri;

        try {
            urlEncodedRedirectUri = URLEncoder.encode(redirectUri, ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Could not URL-encode the redirectUri: " + redirectUri);
        }

        String host = parameters.host;
        String data = "grant_type=authorization_code"
                + "&realm=/"
                + "&redirect_uri=" + urlEncodedRedirectUri
                + "&code=" + authorizationCode;
        log.debug("Requesting tokens by POST to " + host);
        return client.target(host + "/access_token")
                .request()
                .header(AUTHORIZATION, TokenProviderUtil.basicCredentials(parameters.username, parameters.password))
                .header(CACHE_CONTROL, "no-cache")
                .post(Entity.entity(data, APPLICATION_FORM_URLENCODED_TYPE))
                ;
    }

    private IdTokenAndRefreshToken extractToken(String responseString) {
        OidcCredential token = new OidcCredential(TokenProviderUtil.findToken(responseString, ID_TOKEN));
        String refreshToken = TokenProviderUtil.findToken(responseString, REFRESH_TOKEN);
        return new IdTokenAndRefreshToken(token, refreshToken);
    }

    @Builder
    public static class Parameters {
        public final String host;
        public final String username;
        public final String password;
    }
}
