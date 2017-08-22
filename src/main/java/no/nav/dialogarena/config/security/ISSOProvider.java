package no.nav.dialogarena.config.security;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.ToString;
import no.nav.brukerdialog.security.oidc.IdTokenAndRefreshTokenProvider;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.dialogarena.config.fasit.TestEnvironment;
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
import static no.nav.dialogarena.config.fasit.FasitUtils.getEnvironmentClass;
import static no.nav.dialogarena.config.fasit.TestEnvironment.T6;
import static org.eclipse.jetty.http.HttpMethod.POST;


public class ISSOProvider {

    public static final String PRIVELIGERT_VEILEDER = "priveligert_veileder";

    public static final String LOGIN_APPLIKASJON = "veilarblogin";
    public static final String KJENT_LOGIN_ADRESSE = String.format("https://app-t6.adeo.no/%s/api/login", LOGIN_APPLIKASJON);
    public static final String KJENT_LOGIN_ADRESSE_Q = String.format("https://app-q6.adeo.no/%s/api/login", LOGIN_APPLIKASJON);
    public static final TestEnvironment DEFAULT_ENVIRONMENT = T6;

    private static final Logger LOGGER = LoggerFactory.getLogger(ISSOProvider.class);

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String ID_TOKEN_COOKIE_NAME = "ID_token";
    static final Set<String> ISSO_COOKIE_NAMES = new HashSet<>(asList(REFRESH_TOKEN_COOKIE_NAME, ID_TOKEN_COOKIE_NAME));

    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("description: \".*\"");

    public static List<HttpCookie> getISSOCookies() {
        return getISSOCookies(getTestAuthorization(), KJENT_LOGIN_ADRESSE);
    }

    public static List<HttpCookie> getISSOCookies(TestUser testUser) {
        return getISSOCookies(getTestAuthorization(), KJENT_LOGIN_ADRESSE, testUser);
    }

    public static List<HttpCookie> getISSOCookies(String authorization, String redirectUrl) {
        return getISSOCookies(authorization, redirectUrl, getPriveligertVeileder());
    }

    public static List<HttpCookie> getISSOCookies(String redirectUrl, TestEnvironment testEnvironment) {
        return getISSOCookies(getTestAuthorization(), redirectUrl, getPriveligertVeileder(), testEnvironment);
    }

    public static List<HttpCookie> getISSOCookies(String authorization, String redirectUrl, TestUser testUser) {
        return getISSOCookies(authorization, redirectUrl, testUser, DEFAULT_ENVIRONMENT);
    }

    public static List<HttpCookie> getISSOCookies(String authorization, String redirectUrl, TestUser testUser, TestEnvironment testEnvironment) {
        return getISSOCookies(getTestUser(testEnvironment), authorization, redirectUrl, testUser, testEnvironment);
    }

    public static List<HttpCookie> getISSOCookies(ServiceUser issoServiceUser, String authorization, String redirectUrl, TestUser testUser, TestEnvironment testEnvironment) {
        LOGGER.info("getting isso-cookies: {} {}", redirectUrl, authorization);
        try {
            return Util.httpClient(httpClient -> {
                return new ISSORequest(issoServiceUser, authorization, redirectUrl, httpClient, testUser, testEnvironment).getCookies();
            });
        } catch (Exception e) {
            throw new RuntimeException(format("Kunne ikke logge inn i [%s] med isso mot: [%s]\n > %s", testEnvironment, redirectUrl, e.getMessage()), e);
        }
    }

    public static TestUser getPriveligertVeileder() {
        return FasitUtils.getTestUser(PRIVELIGERT_VEILEDER);
    }

    public static String getISSOToken() {
        return getISSOToken(getTestUser());
    }

    public static String getISSOToken(ServiceUser issoServiceUser) {
        return getISSOToken(issoServiceUser, KJENT_LOGIN_ADRESSE);
    }

    public static String getISSOToken(ServiceUser issoServiceUser, String redirectUrl) {
        return getISSOToken(issoServiceUser, redirectUrl, getTestAuthorization());
    }

    public static String getISSOToken(ServiceUser issoServiceUser, String redirectUrl, String authorization) {
        return getISSOToken(issoServiceUser, redirectUrl, authorization, getPriveligertVeileder());
    }

    public static String getISSOToken(ServiceUser issoServiceUser, String redirectUrl, String authorization, TestUser testUser) {
        LOGGER.info("getting isso-token: {}");
        String environment = issoServiceUser.environment;
        try {
            return Util.httpClient(httpClient -> {
                return new ISSORequest(issoServiceUser, authorization, redirectUrl, httpClient, testUser, environment).getToken();
            });
        } catch (Exception e) {
            throw new RuntimeException(format("Kunne ikke logge inn i [%s] med isso mot: [%s]\n > %s", environment, redirectUrl, e.getMessage()), e);
        }
    }

    public static String getTestAuthorization() {
        return FasitUtils.getTestUser("kerberos_test_token").password;
    }

    public static ServiceUser getTestUser() {
        return getTestUser(T6);
    }

    public static ServiceUser getTestUser(TestEnvironment testEnvironment) {
        return FasitUtils.getServiceUser(DEFAULT_ISSO_RP_USER, LOGIN_APPLIKASJON, testEnvironment);
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();


    @ToString
    private static class ISSORequest {

        private final String authorization;
        private final String redirectUrl;
        private final String state = UUID.randomUUID().toString();
        private final HttpClient httpClient;
        private final TestUser testUser;
        private final ServiceUser issoServiceUser;
        private final String issoBasePath;

        private ObjectNode authJson;
        private String tokenId;
        private String appLoginUrl;
        private String authorizationCode;

        private ISSORequest(ServiceUser issoServiceUser, String authorization, String redirectUrl, HttpClient httpClient, TestUser testUser, TestEnvironment environment) {
            this(issoServiceUser, authorization, redirectUrl, httpClient, testUser, environment.toString());
        }

        private ISSORequest(ServiceUser issoServiceUser, String authorization, String redirectUrl, HttpClient httpClient, TestUser testUser, String environment) {
            this.issoServiceUser = issoServiceUser;
            this.httpClient = httpClient;
            this.authorization = authorization;
            this.redirectUrl = redirectUrl;
            this.testUser = testUser;
            this.issoBasePath = String.format("https://isso-%s.adeo.no/isso", getEnvironmentClass(environment));
        }

        private String getToken() {
            startAuth();
            fetchTokenId();
            authorizeWithOauth();
            return retrieveToken();
        }

        private String retrieveToken() {
            String issoHostUrl = issoUrl("/oauth2");
            LOGGER.info("retrieving token from: {}", issoHostUrl);
            IdTokenAndRefreshTokenProvider.Parameters parameters = IdTokenAndRefreshTokenProvider.Parameters.builder()
                    .host(issoHostUrl)
                    .username(issoServiceUser.username)
                    .password(issoServiceUser.password)
                    .build();

            return new IdTokenAndRefreshTokenProvider(parameters)
                    .getToken(authorizationCode, redirectUrl)
                    .getIdToken()
                    .getToken();
        }

        private String issoUrl(String path) {
            return String.format("%s" + path, issoBasePath);
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
            Request request = httpClient.newRequest(issoUrl("/json/authenticate"))
                    .method(POST)
                    .param("realm", "/")
                    .param("goto", issoUrl("/oauth2/authorize?session=winssochain"))
                    .param("authIndexType", "service")
                    .param("authIndexValue", "winssochain")
                    .param("response_type", "code")
                    .param("scope", "openid")
                    .param("client_id", getClientId())
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
                    .newRequest(issoUrl("/oauth2/authorize"))
                    .param("session", "winssochain")
                    .param("authIndexType", "service")
                    .param("authIndexValue", "winssochain")
                    .param("response_type", "code")
                    .param("scope", "openid")
                    .param("client_id", getClientId())
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
                    .filter(a -> "code".equals(a.getName()))
                    .map(NameValuePair::getValue)
                    .findFirst()
                    .get();
        }


        private String getClientId() {
            return issoServiceUser.getUsername();
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
