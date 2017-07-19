package no.nav.brukerdialog.security.oidc;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

class OpenAmUtils {

    static String getSessionToken(String username, String password, String authorizeUrl) {
        Response response = ClientBuilder.newBuilder().build()
                .target(authorizeUrl)
                .request()
                .header("X-OpenAM-Username", username)
                .header("X-OpenAM-Password", password)
                .header("Content-Type", "application/json")
                .buildPost(Entity.json("{}"))
                .invoke();

        return (String) Optional.ofNullable(response.readEntity(Map.class))
                .map( map -> map.get("tokenId"))
                .orElseThrow(() -> new OidcTokenException("Ingen session token i responsen"));
    }

    static String getAuthorizationCode(String openAmHost, String sessionToken, String clientId, String redirectUri) {
        String cookie = "nav-isso=" + sessionToken;
        String uri = openAmHost + "/authorize";
        String encodedRedirectUri;
        try {
            encodedRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Could not URL-encode the redirectUri: " + redirectUri);
        }

        Response response = ClientBuilder.newBuilder().build()
                .target(uri)
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
            throw new OidcTokenException("Feil ved henting av authorization code, fikk status: "+response.getStatus()+" forventet 302");
        }

        String resolvedUri = response.getLocation().getQuery();

        return Arrays.stream(resolvedUri.split("&"))
                .filter( s -> s.contains("code="))
                .map(s -> s.replace("code=",""))
                .findFirst()
                .orElseThrow(() -> new OidcTokenException("Fant ikke authorization code i: "+ resolvedUri));
    }
}
