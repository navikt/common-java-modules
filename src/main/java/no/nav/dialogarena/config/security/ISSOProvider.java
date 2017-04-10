package no.nav.dialogarena.config.security;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.ToString;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestUser;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.glassfish.jersey.client.ClientProperties.FOLLOW_REDIRECTS;


public class ISSOProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ISSOProvider.class);

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String ID_TOKEN_COOKIE_NAME = "ID_token";

    private static final JerseyClient JERSEY_CLIENT = new JerseyClientBuilder()
            .property(FOLLOW_REDIRECTS, false)
            .build();

    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("description: \".*\"");

    public static List<HttpCookie> getISSOCookies(String authorization, String redirectUrl) {
        LOGGER.info("getting isso-cookies: {} {}", redirectUrl, authorization);
        try {
            return new Request(authorization, redirectUrl).execute();
        } catch (Exception e) {
            throw new RuntimeException(format("Kunne ikke logge inn med isso mot: [%s]\n > %s", redirectUrl, e.getMessage()), e);
        }
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();


    @ToString
    private static class Request {

        private final String authorization;
        private final String redirectUrl;
        private final String state = UUID.randomUUID().toString();
        private final List<Cookie> authCookies = new ArrayList<>();

        private ObjectNode authJson;
        private String tokenId;
        private String appLoginUrl;
        private List<HttpCookie> issoCookies;

        private Request(String authorization, String redirectUrl) {
            this.authorization = authorization;
            this.redirectUrl = redirectUrl;
        }

        private List<HttpCookie> execute() {
            startAuth();
            fetchTokenId();
            authorizeWithOauth();
            appLogin();
            return issoCookies;
        }

        private void fetchTokenId() {
            String loginJson = buildLoginJson();
            Response response = sjekk(authRequest().post(entity(loginJson, APPLICATION_JSON_TYPE)));
            String responseString = response.readEntity(String.class);
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
            Response response = sjekk(authRequest().post(null));
            authJson = read(response.readEntity(String.class));
        }

        private Invocation.Builder authRequest() {
            JerseyInvocation.Builder requestBuilder = JERSEY_CLIENT
                    .target("https://isso-t.adeo.no/isso/json/authenticate")
                    .queryParam("realm", "/")
                    .queryParam("goto", "https://isso-t.adeo.no/isso/oauth2/authorize?session=winssochain")
                    .queryParam("authIndexType", "service")
                    .queryParam("authIndexValue", "winssochain")
                    .queryParam("response_type", "code")
                    .queryParam("scope", "openid")
                    .queryParam("client_id", "OIDC")
                    .queryParam("state", state)
                    .queryParam("redirect_uri", redirectUrl)
                    .request()
                    .header("Authorization", authorization);
            this.authCookies.forEach(requestBuilder::cookie);
            return requestBuilder;
        }

        private void authorizeWithOauth() {
            Response response = JERSEY_CLIENT
                    .target("https://isso-t.adeo.no/isso/oauth2/authorize")
                    .queryParam("session", "winssochain")
                    .queryParam("authIndexType", "service")
                    .queryParam("authIndexValue", "winssochain")
                    .queryParam("response_type", "code")
                    .queryParam("scope", "openid")
                    .queryParam("client_id", "OIDC")
                    .queryParam("state", state)
                    .queryParam("redirect_uri", redirectUrl)
                    .request()
                    .cookie("nav-isso", tokenId)
                    .get();
            sjekk(response, 302);
            this.appLoginUrl = response.getHeaderString(LOCATION);
        }

        private void appLogin() {
            Response loginResponse = JERSEY_CLIENT.target(appLoginUrl).request()
                    .cookie(state, redirectUrl)
                    .get();
            sjekk(loginResponse, 307);
            NewCookie refreshTokenCookie = loginResponse.getCookies().get(REFRESH_TOKEN_COOKIE_NAME);
            NewCookie idTokenCookie = loginResponse.getCookies().get(ID_TOKEN_COOKIE_NAME);
            this.issoCookies = Stream.of(refreshTokenCookie, idTokenCookie)
                    .map(newCookie -> new HttpCookie(newCookie.getName(), newCookie.getValue()))
                    .collect(Collectors.toList());
        }

        private Response sjekk(Response response) {
            return sjekk(response, 200);
        }

        private Response sjekk(Response response, int expectedStatus) {
            authCookies.addAll(response.getCookies().values());

            int status = response.getStatus();
            if (status != expectedStatus) {
                String errorMessage = response.readEntity(String.class);
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
