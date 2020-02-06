package no.nav.testconfig.security;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.ToString;
import no.nav.brukerdialog.security.oidc.IdTokenAndRefreshTokenProvider;
import no.nav.brukerdialog.security.oidc.IdTokenAndRefreshTokenProviderConfig;
import no.nav.fasit.FasitUtils;
import no.nav.fasit.ServiceUser;
import no.nav.fasit.TestEnvironment;
import no.nav.fasit.TestUser;
import no.nav.testconfig.util.Util;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.glassfish.jersey.client.ClientRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.HttpCookie;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static no.nav.fasit.FasitUtils.getDefaultEnvironment;
import static no.nav.fasit.FasitUtils.getDefaultTestEnvironment;
import static no.nav.fasit.FasitUtils.getEnvironmentClass;


public class ISSOProvider {

    public static final String PRIVELIGERT_VEILEDER = "priveligert_veileder";

    public static final String DEFAULT_ISSO_RP_USER = "isso-rp-user";
    public static final String LOGIN_APPLIKASJON = "veilarblogin";

    private static final Logger LOGGER = LoggerFactory.getLogger(ISSOProvider.class);

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String ID_TOKEN_COOKIE_NAME = "ID_token";
    static final Set<String> ISSO_COOKIE_NAMES = new HashSet<>(asList(REFRESH_TOKEN_COOKIE_NAME, ID_TOKEN_COOKIE_NAME));

    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("description: \".*\"");

    public static List<HttpCookie> getISSOCookies() {
        return getISSOCookies(getDefaultRedirectUrl());
    }

    public static List<HttpCookie> getISSOCookies(TestUser testUser) {
        return getISSOCookies(getDefaultRedirectUrl(), testUser);
    }

    public static List<HttpCookie> getISSOCookies(String redirectUrl) {
        return getISSOCookies(redirectUrl, getPriveligertVeileder());
    }

    public static List<HttpCookie> getISSOCookies(String redirectUrl, TestEnvironment testEnvironment) {
        return getISSOCookies(redirectUrl, getPriveligertVeileder(), testEnvironment);
    }

    public static List<HttpCookie> getISSOCookies(String redirectUrl, TestUser testUser) {
        return getISSOCookies(redirectUrl, testUser, getDefaultTestEnvironment());
    }

    public static List<HttpCookie> getISSOCookies(String redirectUrl, TestUser testUser, TestEnvironment testEnvironment) {
        return getISSOCookies(getTestUser(testEnvironment), redirectUrl, testUser, testEnvironment);
    }

    public static List<HttpCookie> getISSOCookies(ServiceUser issoServiceUser, String redirectUrl, TestUser testUser, TestEnvironment testEnvironment) {
        LOGGER.info("getting isso-cookies: {} {}", redirectUrl);
        try {
            return Util.httpClient(httpClient -> {
                return new ISSORequest(issoServiceUser, redirectUrl, httpClient, testUser, testEnvironment).getCookies();
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

    public static String getISSOToken(TestUser testUser) {
        return getISSOToken(getTestUser(), getDefaultRedirectUrl(),  testUser);
    }

    public static String getISSOToken(ServiceUser issoServiceUser) {
        return getISSOToken(issoServiceUser, getDefaultRedirectUrl());
    }

    public static String getISSOToken(ServiceUser issoServiceUser, String redirectUrl) {
        return getISSOToken(issoServiceUser, redirectUrl, getPriveligertVeileder());
    }

    public static String getISSOToken(ServiceUser issoServiceUser, String redirectUrl, TestUser testUser) {
        LOGGER.info("getting isso-token: {}");
        String environment = issoServiceUser.environment;
        try {
            return Util.httpClient(httpClient -> {
                return new ISSORequest(issoServiceUser, redirectUrl, httpClient, testUser, environment).getToken();
            });
        } catch (Exception e) {
            throw new RuntimeException(format("Kunne ikke logge inn i [%s] med isso mot: [%s]\n > %s", environment, redirectUrl, e.getMessage()), e);
        }
    }


    public static ServiceUser getTestUser() {
        return getTestUser(getDefaultTestEnvironment());
    }

    public static ServiceUser getTestUser(TestEnvironment testEnvironment) {
        return FasitUtils.getServiceUser(DEFAULT_ISSO_RP_USER, LOGIN_APPLIKASJON, testEnvironment);
    }

    public static String getDefaultRedirectUrl() {
        return getRedirectUrl(getDefaultEnvironment());
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String getRedirectUrl(String environment) {
        return String.format("https://app-%s.adeo.no/%s/api/login", environment, LOGIN_APPLIKASJON);
    }


    @ToString(doNotUseGetters = true)
    private static class ISSORequest {

        private final String redirectUrl;
        private final String state = UUID.randomUUID().toString();
        private final Client httpClient;
        private final TestUser testUser;
        private final ServiceUser issoServiceUser;
        private final String issoBasePath;

        private ObjectNode authJson;
        private String tokenId;
        private String appLoginUrl;
        private String authorizationCode;
        private Map<String, NewCookie> cookies = new HashMap<>();

        private ISSORequest(ServiceUser issoServiceUser, String redirectUrl, Client httpClient, TestUser testUser, TestEnvironment environment) {
            this(issoServiceUser, redirectUrl, httpClient, testUser, environment.toString());
        }

        private ISSORequest(ServiceUser issoServiceUser, String redirectUrl, Client httpClient, TestUser testUser, String environment) {
            this.issoServiceUser = issoServiceUser;
            this.httpClient = httpClient;
            this.redirectUrl = redirectUrl;
            this.testUser = testUser;
            this.issoBasePath = String.format("https://isso-%s.adeo.no/isso", getEnvironmentClass(environment));

            // innlogging er statefull (!) så viktig at vi sender cookies
            this.httpClient.register((ClientRequestFilter) requestContext -> {
                ClientRequest clientRequest = (ClientRequest) requestContext;
                ISSORequest.this.cookies.forEach((s, newCookie) -> clientRequest.cookie(new Cookie(newCookie.getName(), newCookie.getValue())));
            });
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
            IdTokenAndRefreshTokenProviderConfig parameters = IdTokenAndRefreshTokenProviderConfig.builder()
                    .issoHostUrl(issoHostUrl)
                    .issoRpUserUsername(issoServiceUser.username)
                    .issoRpUserPassword(issoServiceUser.password)
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
            return appLogin();
        }

        private void fetchTokenId() {
            String loginJson = buildLoginJson();
            Response response = sjekk(authRequest(loginJson));
            String responseString = response.readEntity(String.class);
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
            Response response = sjekk(authRequest(null));
            authJson = read(response.readEntity(String.class));
        }

        @SneakyThrows
        private Response authRequest(String json) {
            return httpClient.target(issoUrl("/json/authenticate"))
                    .queryParam("realm", "/")
                    .queryParam("goto", issoUrl("/oauth2/authorize?session=winssochain"))
                    .queryParam("authIndexType", "service")
                    .queryParam("authIndexValue", "winssochain")
                    .queryParam("response_type", "code")
                    .queryParam("scope", "openid")
                    .queryParam("client_id", getClientId())
                    .queryParam("state", state)
                    .queryParam("redirect_uri", redirectUrl)
                    .request()
                    .header("Authorization", UUID.randomUUID().toString())
                    .post(json != null ? Entity.json(json) : null);
        }

        @SneakyThrows
        private void authorizeWithOauth() {
            Response response = httpClient
                    .target(issoUrl("/oauth2/authorize"))
                    .queryParam("session", "winssochain")
                    .queryParam("authIndexType", "service")
                    .queryParam("authIndexValue", "winssochain")
                    .queryParam("response_type", "code")
                    .queryParam("scope", "openid")
                    .queryParam("client_id", getClientId())
                    .queryParam("state", state)
                    .queryParam("redirect_uri", redirectUrl)
                    .request()
                    .cookie("nav-isso", tokenId)
                    .get();
            sjekk(response, 302);
            String redirectUrl = response.getHeaderString(LOCATION);
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
        private List<HttpCookie> appLogin() {
            Response loginResponse = httpClient.target(appLoginUrl)
                    .request()
                    .cookie(state, redirectUrl)
                    .get();
            sjekk(loginResponse, 307);
            return loginResponse.getCookies().values()
                    .stream()
                    .filter(httpCookie -> ISSO_COOKIE_NAMES.contains(httpCookie.getName()))
                    .map(n -> new HttpCookie(n.getName(), n.getValue()))
                    .collect(toList());
        }

        private Response sjekk(Response response) {
            return sjekk(response, 200);
        }

        private Response sjekk(Response response, int expectedStatus) {
            cookies.putAll(response.getCookies());
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
