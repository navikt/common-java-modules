package no.nav.dialogarena.config.security;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.ToString;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestUser;
import no.nav.dialogarena.config.util.Util;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpCookie;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static org.eclipse.jetty.http.HttpMethod.POST;


public class ISSOProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ISSOProvider.class);

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String ID_TOKEN_COOKIE_NAME = "ID_token";
    static final Set<String> ISSO_COOKIE_NAMES = new HashSet<>(asList(REFRESH_TOKEN_COOKIE_NAME, ID_TOKEN_COOKIE_NAME));

    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("description: \".*\"");

    public static List<HttpCookie> getISSOCookies(String authorization, String redirectUrl) {
        LOGGER.info("getting isso-cookies: {} {}", redirectUrl, authorization);
        try {
            return Util.httpClient(httpClient -> new ISSORequest(authorization, redirectUrl, httpClient).execute());
        } catch (Exception e) {
            throw new RuntimeException(format("Kunne ikke logge inn med isso mot: [%s]\n > %s", redirectUrl, e.getMessage()), e);
        }
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();


    @ToString
    private static class ISSORequest {

        private final String authorization;
        private final String redirectUrl;
        private final String state = UUID.randomUUID().toString();
        private final HttpClient httpClient;

        private ObjectNode authJson;
        private String tokenId;
        private String appLoginUrl;

        private ISSORequest(String authorization, String redirectUrl, HttpClient httpClient) {
            this.httpClient = httpClient;
            this.authorization = authorization;
            this.redirectUrl = redirectUrl;
        }

        private List<HttpCookie> execute() {
            startAuth();
            fetchTokenId();
            authorizeWithOauth();
            appLogin();
            return httpClient.getCookieStore()
                    .getCookies()
                    .stream()
                    .filter(httpCookie -> ISSO_COOKIE_NAMES.contains(httpCookie.getName()))
                    .collect(toList());
        }

        private void fetchTokenId() {
            String loginJson = buildLoginJson();
            ContentResponse response = sjekk(authRequest(loginJson));
            String responseString = response.getContentAsString();
            JsonNode responseJson = read(responseString);
            this.tokenId = responseJson.get("tokenId").asText();
        }

        private String buildLoginJson() {
            TestUser testUser = FasitUtils.getTestUser("priveligert_veileder");
            JsonNode callbacks = authJson.get("callbacks");
            setInputValue(callbacks.get(0), testUser.username);
            setInputValue(callbacks.get(1), testUser.password);
            return authJson.toString();
        }

        private void setInputValue(JsonNode callbackObject, String value) {
            ObjectNode inputObject = (ObjectNode) callbackObject.get("input").get(0);
            inputObject.put("value", value);
        }

        private void startAuth() {
            ContentResponse response = sjekk(authRequest(null));
            authJson = read(response.getContentAsString());
        }

        @SneakyThrows
        private ContentResponse authRequest(String json) {
            Request request = httpClient.newRequest("https://isso-t.adeo.no/isso/json/authenticate")
                    .method(POST)
                    .param("realm", "/")
                    .param("goto", "https://isso-t.adeo.no/isso/oauth2/authorize?session=winssochain")
                    .param("authIndexType", "service")
                    .param("authIndexValue", "winssochain")
                    .param("response_type", "code")
                    .param("scope", "openid")
                    .param("client_id", "OIDC")
                    .param("state", state)
                    .param("redirect_uri", redirectUrl)
                    .header("Authorization", authorization);
            if (json != null) {
                request.header("Content-Type", "application/json");
                request.content(new StringContentProvider(json));
            }
            return request.send();
        }

        @SneakyThrows
        private void authorizeWithOauth() {
            ContentResponse response = httpClient
                    .newRequest("https://isso-t.adeo.no/isso/oauth2/authorize")
                    .param("session", "winssochain")
                    .param("authIndexType", "service")
                    .param("authIndexValue", "winssochain")
                    .param("response_type", "code")
                    .param("scope", "openid")
                    .param("client_id", "OIDC")
                    .param("state", state)
                    .param("redirect_uri", redirectUrl)
                    .cookie(new HttpCookie("nav-isso", tokenId))
                    .send();
            sjekk(response, 302);
            this.appLoginUrl = response.getHeaders().get(LOCATION);
        }

        @SneakyThrows
        private void appLogin() {
            ContentResponse loginResponse = httpClient.newRequest(appLoginUrl)
                    .cookie(new HttpCookie(state, redirectUrl))
                    .send();
            sjekk(loginResponse, 307);
        }

        private ContentResponse sjekk(ContentResponse response) {
            return sjekk(response, 200);
        }

        private ContentResponse sjekk(ContentResponse response, int expectedStatus) {
            int status = response.getStatus();
            if (status != expectedStatus) {
                String errorMessage = response.getContentAsString();
                Matcher matcher = DESCRIPTION_PATTERN.matcher(errorMessage);
                if (matcher.find()) {
                    errorMessage = matcher.group();
                }
                throw new IllegalStateException(format("not successful expected [%s] was [%s]\n%s\n\n%s",
                        expectedStatus,
                        status,
                        this,
                        errorMessage
                ));
            }
            return response;
        }
    }

    @SneakyThrows
    private static ObjectNode read(String responseString) {
        return (ObjectNode) objectMapper.reader().readTree(responseString);
    }

}
