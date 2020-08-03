package no.nav.common.sts;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.utils.AuthUtils;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import static javax.ws.rs.core.HttpHeaders.*;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;

public class OpenAmUtils {

    private static final String authenticateUri = "json/authenticate?authIndexType=service&authIndexValue=adminconsoleservice";

    // The full URL should look something like this: https://isso-q.adeo.no/isso/json/authenticate?authIndexType=service&authIndexValue=adminconsoleservice
    private static String lagHentSessionTokenUrl(String authorizationUrl) {
        return authorizationUrl.replace("oauth2/authorize", authenticateUri);
    }

    public static String getSessionToken(String username, String password, String authorizationUrl, OkHttpClient client) throws IOException {
        String sessionTokenUrl = lagHentSessionTokenUrl(authorizationUrl);

        Request request = new Request.Builder()
                .url(sessionTokenUrl)
                .header("X-OpenAM-Username", username)
                .header("X-OpenAM-Password", password)
                .post(RequestBody.create(MEDIA_TYPE_JSON, "{}"))
                .build();

        try (Response response = client.newCall(request).execute()) {
            Optional<String> jsonStr = RestUtils.getBodyStr(response);

            if (!jsonStr.isPresent()) {
                throw new IllegalStateException("Body is missing from response");
            }

            JsonNode node = JsonUtils.getMapper().readTree(jsonStr.get());

            return Optional.ofNullable(node.get("tokenId").asText(null))
                    .orElseThrow(() -> new IllegalStateException("Fant ikke 'tokenId' i responsen"));
        }
    }

    public static String getAuthorizationCode(String openAmAuthorizeUrl, String sessionToken, String clientId, String redirectUri, OkHttpClient client) throws IOException {
        String cookie = "nav-isso=" + sessionToken;
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String fullUrl = openAmAuthorizeUrl + String.format("?response_type=code&scope=openid&client_id=%s&redirect_uri=%s", clientId, encodedRedirectUri);

        Request request = new Request.Builder()
                .url(fullUrl)
                .header(COOKIE, cookie)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if(response.code() != 302) {
                throw new RuntimeException("Feil ved henting av authorization code, fikk status: " + response.code() + " forventet 302");
            }

            String redirectLocation = response.header("Location");
            String queryParams = redirectLocation.substring(redirectLocation.indexOf("?") + 1);

            return Arrays.stream(queryParams.split("&"))
                    .filter( s -> s.contains("code="))
                    .map(s -> s.replace("code=",""))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Fant ikke authorization code i: " + redirectLocation));
        }
    }


    public static String exchangeCodeForToken(
            String authorizationCode, String tokenUrl, String redirectUri,
            String issoRpUserUsername, String issoRpUserPassword, OkHttpClient client
    ) throws IOException {
        String urlEncodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String data = "grant_type=authorization_code"
                + "&realm=/"
                + "&redirect_uri=" + urlEncodedRedirectUri
                + "&code=" + authorizationCode;

        Request request = new Request.Builder()
                .url(tokenUrl)
                .header(AUTHORIZATION, AuthUtils.basicCredentials(issoRpUserUsername, issoRpUserPassword))
                .header(CACHE_CONTROL, "no-cache")
                .post(RequestBody.create(MediaType.get("application/x-www-form-urlencoded"), data))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if(response.code() != 200) {
                throw new RuntimeException("Feil ved utveksling av code mot token, fikk status: " + response.code() + " forventet 200");
            }

            Optional<String> jsonStr = RestUtils.getBodyStr(response);

            if (!jsonStr.isPresent()) {
                throw new IllegalStateException("Body is missing from response");
            }

            JsonNode node = JsonUtils.getMapper().readTree(jsonStr.get());

            return Optional.ofNullable(node.get("id_token").asText(null))
                    .orElseThrow(() -> new IllegalStateException("Fant ikke 'id_token' i responsen"));
        }
    }

}
