package no.nav.brukerdialog.filter;

import no.nav.brukerdialog.security.domain.IdTokenAndRefreshToken;
import no.nav.brukerdialog.security.oidc.IdTokenAndRefreshTokenProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.mockito.Mockito;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OpenAMHelper {

    private String openAmHost;

    public OpenAMHelper(String openAmHost) {
        this.openAmHost = openAmHost;
    }

    public String getAuthorizationCode(String username, String password) throws IOException, ScriptException, InterruptedException {
        String sessionId = hentSession();

        String sessionToken = hentSessionToken(sessionId, username, password);
        return hentAuthorizationCode(sessionToken);
    }

    public IdTokenAndRefreshToken getTokens(String username, String password) throws IOException, ScriptException, InterruptedException {
        String authorizationCode = getAuthorizationCode(username, password);
        return hentTokens(authorizationCode);
    }

    private String hentSession() throws IOException, ScriptException {
        String url = openAmHost + "/openam/json/authenticate?=undefined";
        Function<String, String> hentAuthIdFraResponse = (result) -> {
            Map<String, String> stringStringMap = parseJSON(result);
            return stringStringMap.get("authId");
        };
        return post(url, null, hentAuthIdFraResponse);
    }

    private String hentSessionToken(String sessionId, String username, String password) throws IOException, ScriptException {
        String data = "{\"authId\":\"" + sessionId + "\",\"template\":\"\",\"stage\":\"DataStore1\",\"header\":\"Sign in to OpenAM\",\"callbacks\":[{\"type\":\"NameCallback\",\"output\":[{\"name\":\"prompt\",\"value\":\"User Name:\"}],\"input\":[{\"name\":\"IDToken1\",\"value\":\"" + username + "\"}]},{\"type\":\"PasswordCallback\",\"output\":[{\"name\":\"prompt\",\"value\":\"Password:\"}],\"input\":[{\"name\":\"IDToken2\",\"value\":\"" + password + "\"}]}]}";
        String url = openAmHost + "/openam/json/authenticate";
        Function<String, String> hentSessionTokenFraResult = (result) -> {
            Map<String, String> stringStringMap = parseJSON(result);
            return stringStringMap.get("tokenId");
        };
        return post(url, data, hentSessionTokenFraResult);
    }

    private static <T> T post(String url, String data, Function<String, T> resultTransformer) throws IOException {
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-type", "application/json");
        if (data != null) {
            post.setEntity(new StringEntity(data, "UTF-8"));
        }
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                String responseString = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
                        .lines()
                        .collect(Collectors.joining("\n"));
                if (response.getStatusLine().getStatusCode() == 200) {
                    return resultTransformer.apply(responseString);
                } else {
                    throw new RuntimeException("Fikk status " + response.getStatusLine().getStatusCode() + " og response: " + responseString);
                }
            }
        }
    }

    private static Map<String, String> parseJSON(String response) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            return (Map<String, String>) engine.eval(String.format("JSON.parse('%s');", response));
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    private static IdTokenAndRefreshToken hentTokens(String authorizationCode) {
        URI uri = URI.create("http://localhost:8080");
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getBaseUri()).thenReturn(uri);
        return new IdTokenAndRefreshTokenProvider().getToken(authorizationCode, uriInfo);
    }

    private String hentAuthorizationCode(String sessionToken) throws IOException {
        String redirect = URLEncoder.encode("http://localhost:8080/eridanus-jwt/cb", "UTF-8");
        String cookie = "nav-isso=" + sessionToken;

        HttpGet get = new HttpGet(openAmHost + "/openam/oauth2/authorize?response_type=code&scope=openid&client_id=OIDC&state=dummy&redirect_uri=" + redirect);
        get.setHeader("Content-type", "application/json");
        get.setHeader("Cookie", cookie);
        try (CloseableHttpClient httpClient = HttpClients.createMinimal()) {
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                Pattern pattern = Pattern.compile("code=([^&]*)");
                String locationHeader = response.getFirstHeader("Location").getValue();
                Matcher matcher = pattern.matcher(locationHeader);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                throw new IllegalArgumentException("Did not find auth.code");
            }
        }
    }
}
