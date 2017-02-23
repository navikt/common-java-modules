package no.nav.security.jwt.security.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.security.jwt.security.domain.IdTokenAndRefreshToken;
import no.nav.security.jwt.security.domain.JwtCredential;
import no.nav.security.jwt.tools.HostUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.stream.Collectors;

public abstract class TokenProvider<T> {

    public static TokenFromAuthorizationCodeProvider fromAuthorizationCode(String authorizationCode, UriInfo uri) {
        return new TokenFromAuthorizationCodeProvider(authorizationCode, uri);
    }

    public static TokenFromRefreshTokenProvider fromRefreshToken(String refreshToken) {
        return new TokenFromRefreshTokenProvider(refreshToken);
    }

    protected abstract HttpUriRequest createTokenRequest();

    protected abstract T extractToken(String responseString);

    public T getToken() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpUriRequest request = createTokenRequest();
            try (CloseableHttpResponse response = client.execute(request)) {
                String responseString = responseText(response);
                if (response.getStatusLine().getStatusCode() == 200) {
                    return extractToken(responseString);
                }
                throw new IllegalArgumentException("Could not refresh token, got " + response.getStatusLine().getStatusCode() + " from auth.server");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String findToken(String responseString, String tokenName) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode json = mapper.readTree(responseString);
            JsonNode token = json.get(tokenName);
            if (token == null) {
                return null;
            }
            return token.textValue();
        } catch (IOException e) {
            return null;
        }
    }

    private String responseText(CloseableHttpResponse response) throws IOException {
        return new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
                .lines()
                .collect(Collectors.joining("\n"));
    }


    String basicCredentials(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", username, password).getBytes());
    }

    static class TokenFromAuthorizationCodeProvider extends TokenProvider<IdTokenAndRefreshToken> {

        private static final Logger log = LoggerFactory.getLogger(TokenFromAuthorizationCodeProvider.class);
        private final String authorizationCode;

        private final UriInfo redirectUri;

        TokenFromAuthorizationCodeProvider(String authorizationCode, UriInfo redirectUri) {
            this.authorizationCode = authorizationCode;
            this.redirectUri = redirectUri;
        }

        @Override
        public IdTokenAndRefreshToken extractToken(String responseString) {
            JwtCredential token = new JwtCredential(findToken(responseString, "id_token"));
            String refreshToken = findToken(responseString, "refresh_token");
            return new IdTokenAndRefreshToken(token, refreshToken);
        }

        protected HttpUriRequest createTokenRequest() {
            String urlEncodedRedirectUri;
            String loginRedirectUrl = System.getProperty("login.redirect.url");
            try {
                urlEncodedRedirectUri = URLEncoder.encode(HostUtils.formatSchemeHostPort(redirectUri) + loginRedirectUrl, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Could not URL-encode the redirectUri: " + redirectUri);
            }

            String host = System.getProperty("isso-host.url");
            String realm = "/";
            String username = System.getProperty("isso-rp-user.username");
            String password = System.getProperty("isso-rp-user.password");

            HttpPost request = new HttpPost(host + "/openam/oauth2/access_token");
            request.setHeader("Authorization", basicCredentials(username, password));
            request.setHeader("Cache-Control", "no-cache");
            request.setHeader("Content-type", "application/x-www-form-urlencoded");
            String data = "grant_type=authorization_code"
                    + "&realm=" + realm
                    + "&redirect_uri=" + urlEncodedRedirectUri
                    + "&code=" + authorizationCode;
            log.info("POST to " + host + " data " + data);
            request.setEntity(new StringEntity(data, "UTF-8"));
            return request;
        }
    }

    static class TokenFromRefreshTokenProvider extends TokenProvider<JwtCredential> {

        private static final Logger log = LoggerFactory.getLogger(TokenFromRefreshTokenProvider.class);
        private final String refreshToken;

        TokenFromRefreshTokenProvider(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        @Override
        public JwtCredential extractToken(String responseString) {
            return new JwtCredential(findToken(responseString, "id_token"));
        }

        protected HttpUriRequest createTokenRequest() {
            String host = System.getProperty("isso-host.url");
            String realm = "/";
            String username = System.getProperty("isso-rp-user.username");
            String password = System.getProperty("isso-rp-user.password");

            HttpPost request = new HttpPost(host + "/openam/oauth2/access_token");
            request.setHeader("Authorization", basicCredentials(username, password));
            request.setHeader("Cache-Control", "no-cache");
            request.setHeader("Content-type", "application/x-www-form-urlencoded");
            String data = "grant_type=refresh_token"
                    + "&scope=openid"
                    + "&realm=" + realm
                    + "&refresh_token=" + refreshToken;
            log.info("POST to " + host + " data " + data);
            request.setEntity(new StringEntity(data, "UTF-8"));
            return request;
        }

    }
}
