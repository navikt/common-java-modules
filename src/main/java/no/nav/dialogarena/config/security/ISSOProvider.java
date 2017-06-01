package no.nav.dialogarena.config.security;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.ToString;
import no.nav.brukerdialog.security.oidc.IdTokenAndRefreshTokenProvider;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.dialogarena.config.fasit.TestUser;
import no.nav.dialogarena.config.util.Util;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
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
import static no.nav.dialogarena.config.DevelopmentSecurity.DEFAULT_ISSO_RP_USER;
import static org.eclipse.jetty.http.HttpMethod.POST;


public class ISSOProvider {

    static final String KJENT_LOGIN_ADRESSE = "https://app-t6.adeo.no/veilarbpersonflatefs/tjenester/login";

    private static final Logger LOGGER = LoggerFactory.getLogger(ISSOProvider.class);

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String ID_TOKEN_COOKIE_NAME = "ID_token";
    static final Set<String> ISSO_COOKIE_NAMES = new HashSet<>(asList(REFRESH_TOKEN_COOKIE_NAME, ID_TOKEN_COOKIE_NAME));

    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("description: \".*\"");

    public static List<HttpCookie> getISSOCookies() {
        return getISSOCookies(getTestAuthorization(),KJENT_LOGIN_ADRESSE);
    }

    public static List<HttpCookie> getISSOCookies(String authorization, String redirectUrl) {
        LOGGER.info("getting isso-cookies: {} {}", redirectUrl, authorization);
        try {
            return Util.httpClient(httpClient -> new ISSORequest(authorization, redirectUrl, httpClient).getCookies());
        } catch (Exception e) {
            throw new RuntimeException(format("Kunne ikke logge inn med isso mot: [%s]\n > %s", redirectUrl, e.getMessage()), e);
        }
    }

    public static String getISSOToken() {
        return getISSOToken(getTestUser(), getTestAuthorization());
    }

    private static ServiceUser getTestUser() {
        return FasitUtils.getServiceUser(DEFAULT_ISSO_RP_USER, "veilarbpersonflatefs", "t6");
    }

    private static String getTestAuthorization() {
        return FasitUtils.getTestUser("kerberos_test_token").password;
    }

    public static String getISSOToken(ServiceUser issoServiceUser, String authorization) {
        LOGGER.info("getting isso-token: {}");
        try {
            return Util.httpClient(httpClient -> new ISSORequest(authorization, KJENT_LOGIN_ADRESSE, httpClient).getToken(issoServiceUser));
        } catch (Exception e) {
            throw new RuntimeException(format("Kunne ikke logge inn med isso mot: [%s]\n > %s", KJENT_LOGIN_ADRESSE, e.getMessage()), e);
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
        private String authorizationCode;

        private ISSORequest(String authorization, String redirectUrl, HttpClient httpClient) {
            this.httpClient = httpClient;
            this.authorization = authorization;
            this.redirectUrl = redirectUrl;
        }

        private String getToken(ServiceUser issoServiceUser) {
            startAuth();
            fetchTokenId();
            authorizeWithOauth();
            return retrieveToken(issoServiceUser);
        }

        private String retrieveToken(ServiceUser issoServiceUser) {
            synchronized (this.getClass()) {
                // TODO Veldig kjipt at IdTokenAndRefreshTokenProvider henter dette fra system properties!
                System.setProperty("isso-host.url", "https://isso-t.adeo.no/isso/oauth2");
                System.setProperty("isso-rp-user.username", issoServiceUser.username);
                System.setProperty("isso-rp-user.password", issoServiceUser.password);

                return new IdTokenAndRefreshTokenProvider().getToken(authorizationCode, redirectUrl)
                        .getIdToken()
                        .getToken();
            }
        }

        private List<HttpCookie> getCookies() {
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
            String redirectUrl = response.getHeaders().get(LOCATION);
            this.appLoginUrl = redirectUrl;
            this.authorizationCode = new URIBuilder(redirectUrl)
                    .getQueryParams()
                    .stream()
                    .filter(a->"code".equals(a.getName()))
                    .map(NameValuePair::getValue)
                    .findFirst()
                    .get();
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
