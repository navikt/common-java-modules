package no.nav.brukerdialog.security.oidc;

import no.nav.brukerdialog.security.domain.IdTokenAndRefreshToken;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.tools.HostUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static java.lang.System.getProperty;

public class IdTokenAndRefreshTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(IdTokenAndRefreshTokenProvider.class);
    private static final String DEFAULT_REDIRECT_URL = "/"+ getProperty("applicationName")+"/tjenester/login";

    public IdTokenAndRefreshToken getToken(String authorizationCode, UriInfo uri) {
        return TokenProviderUtil.getToken(() -> createTokenRequest(authorizationCode, uri), s -> extractToken(s));
    }

    private HttpUriRequest createTokenRequest(String authorizationCode, UriInfo redirectUri) {
        String redirectUrl = getProperty("oidc-redirect.url") == null ? DEFAULT_REDIRECT_URL : "/oidclogin/login";
        String urlEncodedRedirectUri;
        try {
            urlEncodedRedirectUri = URLEncoder.encode(HostUtils.formatSchemeHostPort(redirectUri) + redirectUrl, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Could not URL-encode the redirectUri: " + redirectUri);
        }

        String host = getProperty("isso-host.url");
        String realm = "/";
        String username = getProperty("isso-rp-user.username");
        String password = getProperty("isso-rp-user.password");

        HttpPost request = new HttpPost(host + "/access_token");
        request.setHeader("Authorization", TokenProviderUtil.basicCredentials(username, password));
        request.setHeader("Cache-Control", "no-cache");
        request.setHeader("Content-type", "application/x-www-form-urlencoded");
        String data = "grant_type=authorization_code"
                + "&realm=" + realm
                + "&redirect_uri=" + urlEncodedRedirectUri
                + "&code=" + authorizationCode;
        log.debug("Requesting tokens by POST to " + host);
        request.setEntity(new StringEntity(data, "UTF-8"));
        return request;
    }

    private IdTokenAndRefreshToken extractToken(String responseString) {
        OidcCredential token = new OidcCredential(TokenProviderUtil.findToken(responseString, "id_token"));
        String refreshToken = TokenProviderUtil.findToken(responseString, "refresh_token");
        return new IdTokenAndRefreshToken(token, refreshToken);
    }

}
