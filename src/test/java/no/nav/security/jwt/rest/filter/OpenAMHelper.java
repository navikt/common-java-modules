package no.nav.security.jwt.rest.filter;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OpenAMHelper {

    static String getJWT(String openAmhost, String username, String password) throws IOException, ScriptException {
        String sessionId = hentSession(openAmhost);
        OpenAmSessionToken openAmToken = hentSessionToken(sessionId, username, password, openAmhost);
        return transformerTokenTilJWT(openAmToken, openAmhost);
    }

    private static String createTranslateRequest(String openAmToken) {
        return "{" +
                "                \"input_token_state\": {\n" +
                "                    \"token_type\": \"OPENAM\",\n" +
                "                    \"session_id\": \"" + openAmToken + "\"\n" +
                "                },\n" +
                "                \"output_token_state\": {\n" +
                "                    \"token_type\": \"OPENIDCONNECT\",\n" +
                "                    \"nonce\": \"" + Math.random() + "\",\n" +
                "                    \"allow_access\": true\n" +
                "                }" +
                "}";
    }

    private static String transformerTokenTilJWT(OpenAmSessionToken openAmToken, String openAmhost) throws IOException, ScriptException {
        String url = openAmhost + "/openam/rest-sts/stsoidc?_action=translate";
        String data = createTranslateRequest(openAmToken.getToken());
        Function<String, String> hentOidcFraResult = (result) -> {
            Map<String, String> json = parseJSON(result);
            return json.get("issued_token");
        };
        return post(url, data, hentOidcFraResult);
    }

    private static String hentSession(String openAmhost) throws IOException, ScriptException {
        String url = openAmhost + "/openam/json/authenticate?=undefined";
        Function<String, String> hentAuthIdFraResponse = (result) -> {
            Map<String, String> stringStringMap = parseJSON(result);
            return stringStringMap.get("authId");
        };
        return post(url, null, hentAuthIdFraResponse);
    }

    private static OpenAmSessionToken hentSessionToken(String sessionId, String username, String password, String openAmhost) throws IOException, ScriptException {
        String data = "{\"authId\":\"" + sessionId + "\",\"template\":\"\",\"stage\":\"DataStore1\",\"header\":\"Sign in to OpenAM\",\"callbacks\":[{\"type\":\"NameCallback\",\"output\":[{\"name\":\"prompt\",\"value\":\"User Name:\"}],\"input\":[{\"name\":\"IDToken1\",\"value\":\"" + username + "\"}]},{\"type\":\"PasswordCallback\",\"output\":[{\"name\":\"prompt\",\"value\":\"Password:\"}],\"input\":[{\"name\":\"IDToken2\",\"value\":\"" + password + "\"}]}]}";
        String url = openAmhost + "/openam/json/authenticate";
        Function<String, OpenAmSessionToken> hentSessionTokenFraResult = (result) -> {
            Map<String, String> stringStringMap = parseJSON(result);
            return new OpenAmSessionToken(stringStringMap.get("tokenId"));
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

    private static class OpenAmSessionToken {

        private String token;

        public OpenAmSessionToken(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }
    }

    public static void main(String[] args) throws IOException, ScriptException {
        String username = "demo";
        String password = "changeit";
        String token = OpenAMHelper.getJWT("https://isso-t.adeo.no", username, password);
        System.out.printf("A JWT token for user '%s' is '%s'\n", username, token);
    }

}
