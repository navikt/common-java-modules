package no.nav.security.jwt.tools;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

public class AuthorizationRequestBuilder {

    private final String server;
    private final String scope = "openid";
    private String redirectLocation;
    private boolean useKerberos = true;
    private boolean redirectToOriginal;
    private String originalUrl;

    public AuthorizationRequestBuilder(HttpServletRequest req) {
        server = HostUtils.formatSchemeHostPort(req);
        originalUrl = getOriginalUrl(req);
    }

    private String getOriginalUrl(HttpServletRequest req) {
        return req.getQueryString() == null
                ? req.getRequestURL().toString()
                : req.getRequestURL().toString() + "?" + req.getQueryString();
    }

    public AuthorizationRequestBuilder(UriInfo uri) {
        server = HostUtils.formatSchemeHostPort(uri);
    }

    public AuthorizationRequestBuilder ikkeBrukKerberos() {
        useKerberos = false;
        return this;
    }

    public AuthorizationRequestBuilder medRedirectToOriginalLocation() {
        redirectToOriginal = true;
        return this;
    }

    public Response build() throws UnsupportedEncodingException {
        NewCookie cookie = new NewCookie("auth-state", getEncodedState(), "/", null, "", NewCookie.DEFAULT_MAX_AGE, true, true);
        Response response = Response.temporaryRedirect(URI.create(buildRedirectString()))
                .header("HostUtils", server)
                .cookie(cookie)
                .build();
        return response;
    }

    public String buildRedirectString() throws UnsupportedEncodingException {
        String clientId = System.getProperty("isso-rp-user.username");
        String state = getEncodedState();
        String redirectUrl = server + System.getProperty("login.redirect.url");
        String kerberosTrigger = useKerberos
                ? "session=winssochain&authIndexType=service&authIndexValue=winssochain&"
                : "";
        return String.format("%s/openam/oauth2/authorize?" + kerberosTrigger + "response_type=code&scope=%s&client_id=%s&state=%s&redirect_uri=%s",
                System.getProperty("isso-host.url"),
                scope,
                URLEncoder.encode(clientId, "UTF-8"),
                state,
                URLEncoder.encode(redirectUrl, "UTF-8")
        );
    }

    private String getEncodedState() throws UnsupportedEncodingException {
        if (redirectToOriginal) {
            return encode(originalUrl);
        } else {
            return "";
        }
    }

    //FIXME hvis URL (inkludert parametre) inneholder sensitive opplysninger bør disse også krypteres
    private String encode(String redirectLocation) throws UnsupportedEncodingException {
        return URLEncoder.encode(redirectLocation, "UTF-8");
    }

}
