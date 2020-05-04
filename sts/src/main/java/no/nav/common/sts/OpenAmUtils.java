package no.nav.common.sts;

import no.nav.common.utils.AuthUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;

public class OpenAmUtils {

    private static final String authenticateUri = "json/authenticate?authIndexType=service&authIndexValue=adminconsoleservice";

    // The full URL should look something like this: https://isso-q.adeo.no/isso/json/authenticate?authIndexType=service&authIndexValue=adminconsoleservice
    private static String lagHentSessionTokenUrl(String authorizationUrl) {
        return authorizationUrl.replace("oauth2/authorize", authenticateUri);
    }

    public static String getSessionToken(String username, String password, String authorizationUrl, Client client) {
        String sessionTokenUrl = lagHentSessionTokenUrl(authorizationUrl);
        Response response = client
                .target(sessionTokenUrl)
                .request()
                .header("X-OpenAM-Username", username)
                .header("X-OpenAM-Password", password)
                .header("Content-Type", "application/json")
                .buildPost(Entity.json("{}"))
                .invoke();

        return (String) Optional.ofNullable(response.readEntity(Map.class))
                .map( map -> map.get("tokenId"))
                .orElseThrow(() -> new IllegalStateException("Ingen session token i responsen"));
    }

    public static String getAuthorizationCode(String openAmAuthorizeUrl, String sessionToken, String clientId, String redirectUri, Client client) {
        String cookie = "nav-isso=" + sessionToken;
        String encodedRedirectUri;
        encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        Response response = client
                .target(openAmAuthorizeUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", encodedRedirectUri)
                .property("jersey.config.client.followRedirects", false)
                .request()
                .header("Content-Type", "application/json")
                .header("Cookie", cookie)
                .buildGet()
                .invoke();

        if(response.getStatus() != 302) {
            throw new RuntimeException("Feil ved henting av authorization code, fikk status: " + response.getStatus() + " forventet 302");
        }

        String resolvedUri = response.getLocation().getQuery();

        return Arrays.stream(resolvedUri.split("&"))
                .filter( s -> s.contains("code="))
                .map(s -> s.replace("code=",""))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Fant ikke authorization code i: "+ resolvedUri));
    }


    public static String exchangeCodeForToken(
            String authorizationCode, String tokenUrl, String redirectUri,
            String issoRpUserUsername, String issoRpUserPassword, Client client
    ) {
        String urlEncodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String data = "grant_type=authorization_code"
                + "&realm=/"
                + "&redirect_uri=" + urlEncodedRedirectUri
                + "&code=" + authorizationCode;

        Response response = client.target(tokenUrl)
                .request()
                .header(AUTHORIZATION, AuthUtils.basicCredentials(issoRpUserUsername, issoRpUserPassword))
                .header(CACHE_CONTROL, "no-cache")
                .post(Entity.entity(data, APPLICATION_FORM_URLENCODED_TYPE));

        if(response.getStatus() != 200) {
            throw new RuntimeException("Feil ved utveksling av code mot token, fikk status: " + response.getStatus() + " forventet 200");
        }

        return (String) Optional.ofNullable(response.readEntity(Map.class))
                .map( map -> map.get("id_token"))
                .orElseThrow(() -> new IllegalStateException("Fant ikke id_token i responsen"));
    }

}
