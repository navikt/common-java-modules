package no.nav.brukerdialog.security.oidc;

import lombok.Builder;
import no.nav.brukerdialog.security.domain.IdTokenAndRefreshToken;
import no.nav.brukerdialog.security.domain.OidcCredential;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static java.lang.System.getProperty;
import static javax.ws.rs.core.HttpHeaders.*;
import static no.nav.brukerdialog.security.Constants.*;

public class IdTokenAndRefreshTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(IdTokenAndRefreshTokenProvider.class);

    static final String ENCODING = "UTF-8";

    public static final String ID_TOKEN = "id_token";
    public static final String REFRESH_TOKEN = "refresh_token";

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
        return TokenProviderUtil.getToken(() -> createTokenRequest(authorizationCode, redirectUri), this::extractToken);
    }

    HttpPost createTokenRequest(String authorizationCode, String redirectUri) {
        String urlEncodedRedirectUri;

        try {
            urlEncodedRedirectUri = URLEncoder.encode(redirectUri, ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Could not URL-encode the redirectUri: " + redirectUri);
        }

        HttpPost request = new HttpPost(parameters.host + "/access_token");
        request.setHeader(AUTHORIZATION, TokenProviderUtil.basicCredentials(parameters.username, parameters.password));
        request.setHeader(CACHE_CONTROL, "no-cache");
        request.setHeader(CONTENT_TYPE, "application/x-www-form-urlencoded");
        String data = "grant_type=authorization_code"
                + "&realm=/"
                + "&redirect_uri=" + urlEncodedRedirectUri
                + "&code=" + authorizationCode;
        log.debug("Requesting tokens by POST to " + parameters.host);
        request.setEntity(new StringEntity(data, ENCODING));
        return request;
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
