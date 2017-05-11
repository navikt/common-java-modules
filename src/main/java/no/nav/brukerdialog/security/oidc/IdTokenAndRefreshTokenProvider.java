package no.nav.brukerdialog.security.oidc;

import lombok.Builder;
import no.nav.brukerdialog.security.domain.IdTokenAndRefreshToken;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.tools.HostUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static java.lang.System.getProperty;
import static javax.ws.rs.core.HttpHeaders.*;

public class IdTokenAndRefreshTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(IdTokenAndRefreshTokenProvider.class);

    public static final String ISSO_HOST_URL_PROPERTY_NAME = "isso-host.url";
    public static final String ISSO_RP_USER_USERNAME_PROPERTY_NAME = "isso-rp-user.username";
    public static final String ISSO_RP_USER_PASSWORD_PROPERTY_NAME = "isso-rp-user.password";

    static final String ENCODING = "UTF-8";

    private static final String DEFAULT_REDIRECT_URL = "/"+ getProperty("applicationName")+"/tjenester/login";
    private final Parameters parameters;

    public IdTokenAndRefreshTokenProvider() {
        this(Parameters.builder()
                // TODO er dette korrekt? eller skal det være: getProperty("oidc-redirect.url") == null ? getProperty("oidc-redirect.url") : "/oidclogin/login";
                .redirectUrl(getProperty("oidc-redirect.url") == null ? DEFAULT_REDIRECT_URL : "/oidclogin/login")
                //
                .host(getProperty(ISSO_HOST_URL_PROPERTY_NAME))
                .username(getProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME))
                .password(getProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME))
                .build()
        );
    }

    public IdTokenAndRefreshTokenProvider(Parameters parameters) {
        this.parameters = parameters;
    }

    public IdTokenAndRefreshToken getToken(String authorizationCode, UriInfo uri) {
        return TokenProviderUtil.getToken(() -> createTokenRequest(authorizationCode, uri), s -> extractToken(s));
    }

    HttpPost createTokenRequest(String authorizationCode, UriInfo redirectUri) {
        String urlEncodedRedirectUri;
        try {
            urlEncodedRedirectUri = URLEncoder.encode(HostUtils.formatSchemeHostPort(redirectUri) + parameters.redirectUrl, ENCODING);
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
        OidcCredential token = new OidcCredential(TokenProviderUtil.findToken(responseString, "id_token"));
        String refreshToken = TokenProviderUtil.findToken(responseString, "refresh_token");
        return new IdTokenAndRefreshToken(token, refreshToken);
    }

    @Builder
    public static class Parameters {

        public final String host;
        public final String redirectUrl;
        public final String username;
        public final String password;

    }

}
