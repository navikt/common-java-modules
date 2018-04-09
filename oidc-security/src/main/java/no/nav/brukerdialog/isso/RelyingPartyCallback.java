package no.nav.brukerdialog.isso;

import no.nav.brukerdialog.filter.DoNotCache;
import no.nav.brukerdialog.security.domain.IdTokenAndRefreshToken;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.oidc.IdTokenAndRefreshTokenProvider;
import no.nav.brukerdialog.security.oidc.OidcTokenValidator;
import no.nav.brukerdialog.tools.HostUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static no.nav.brukerdialog.security.Constants.ID_TOKEN_COOKIE_NAME;
import static no.nav.brukerdialog.security.Constants.REFRESH_TOKEN_COOKIE_NAME;

@Path("login")
@DoNotCache
public class RelyingPartyCallback {
    private static final Logger log = LoggerFactory.getLogger(RelyingPartyCallback.class);

    private IdTokenAndRefreshTokenProvider tokenProvider = new IdTokenAndRefreshTokenProvider();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogin(@QueryParam("code") String authorizationCode, @QueryParam("state") String state, @Context UriInfo uri, @Context HttpHeaders headers) {
        if (authorizationCode == null) {
            log.error("URL parameter 'code' is missing");
            return Response.status(BAD_REQUEST).build();
        }
        if (state == null) {
            log.error("URL parameter 'state' is missing");
            return Response.status(BAD_REQUEST).build();
        }

        Cookie redirect = headers.getCookies().get(state);
        if (redirect == null || redirect.getValue() == null || redirect.getValue().isEmpty()) {
            log.error("Cookie for redirectionURL is missing or empty");
            return Response.status(BAD_REQUEST).build();
        }

        IdTokenAndRefreshToken tokens = tokenProvider.getToken(authorizationCode, uri.getAbsolutePath().toString());
        OidcCredential token = tokens.getIdToken();
        String refreshToken = tokens.getRefreshToken();

        OidcTokenValidator.OidcTokenValidatorResult result = new OidcTokenValidator().validate(token.getToken());

        if (!result.isValid()) {
            return Response.status(FORBIDDEN).build();
        }

        boolean sslOnlyCookie = !Boolean.valueOf(System.getProperty("develop-local", "false"));
        String cookieDomain = HostUtils.cookieDomain(uri);
        NewCookie tokenCookie = new NewCookie(ID_TOKEN_COOKIE_NAME, token.getToken(), "/", cookieDomain, "", NewCookie.DEFAULT_MAX_AGE, sslOnlyCookie, true);
        NewCookie refreshTokenCookie = new NewCookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken, "/", cookieDomain, "", NewCookie.DEFAULT_MAX_AGE, sslOnlyCookie, true);
        NewCookie deleteOldStateCookie = new NewCookie(state, "", "/", null, "", 0, sslOnlyCookie, true);

        Response.ResponseBuilder responseBuilder;
        //TODO CSRF attack protection. See RFC-6749 section 10.12 (the state-cookie containing redirectURL shold be encrypted to avoid tampering)
        String originalUrl = urlDecode(redirect.getValue());
        responseBuilder = Response.temporaryRedirect(URI.create(originalUrl));
        responseBuilder.cookie(tokenCookie);
        responseBuilder.cookie(refreshTokenCookie);
        responseBuilder.cookie(deleteOldStateCookie);
        return responseBuilder.build();
    }


    private static String urlDecode(String urlEncoded) {
        try {
            return URLDecoder.decode(urlEncoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Could not URLdecode: " + urlEncoded);
        }
    }

}
