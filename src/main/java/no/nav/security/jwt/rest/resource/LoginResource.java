package no.nav.security.jwt.rest.resource;

import no.nav.security.jwt.rest.filter.DoNotCache;
import no.nav.security.jwt.security.domain.IdTokenAndRefreshToken;
import no.nav.security.jwt.security.domain.JwtCredential;
import no.nav.security.jwt.security.jwks.JwksKeyHandler;
import no.nav.security.jwt.security.jwks.JwksKeyHandlers;
import no.nav.security.jwt.security.oidc.OidcTokenValidator;
import no.nav.security.jwt.security.oidc.TokenProvider;
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

@Path("login")
@DoNotCache
public class LoginResource {

    private static final Logger log = LoggerFactory.getLogger(LoginResource.class);

    private static final JwksKeyHandler jwks = JwksKeyHandlers.createKeyHandler();

    @QueryParam("code")
    private String authorizationCode;

    @QueryParam("state")
    private String state;

    @Context
    UriInfo uri;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogin() {
        if (authorizationCode == null) {
            log.error("URL parameter 'code' is missing");
            return Response.status(BAD_REQUEST).build();
        }
        if (state == null) {
            log.error("URL parameter 'state' is missing");
            return Response.status(BAD_REQUEST).build();
        }

        //TODO CSRF attack protection. See RFC-6749 section 10.12

        TokenProvider<IdTokenAndRefreshToken> tokenProvider = TokenProvider.fromAuthorizationCode(authorizationCode, uri);
        IdTokenAndRefreshToken tokens = tokenProvider.getToken();
        JwtCredential token = tokens.getIdToken();
        String refreshToken = tokens.getRefreshToken();

        OidcTokenValidator validator = new OidcTokenValidator(jwks, token.getToken());
        if (!validator.tokenIsValid()) {
            return Response.status(FORBIDDEN).build();
        }

        String contextRoot = "/"+System.getProperty("applicationName");
        boolean sslOnlyCookie = !Boolean.valueOf(System.getProperty("develop-local", "false"));
        NewCookie tokenCookie = new NewCookie("Authentication", token.getToken(), contextRoot, null, "", NewCookie.DEFAULT_MAX_AGE, sslOnlyCookie, true);
        NewCookie refreshTokenCookie = new NewCookie("AuthenticationRefresh", refreshToken, contextRoot, null, "", NewCookie.DEFAULT_MAX_AGE, sslOnlyCookie, true);
        NewCookie tokenExpirationCookie = new NewCookie("AuthenticationExpiration", Long.toString(validator.getExp()), contextRoot, null, "", NewCookie.DEFAULT_MAX_AGE, sslOnlyCookie, false);

        boolean shouldRedirect = !state.isEmpty();

        Response.ResponseBuilder responseBuilder;
        if (shouldRedirect) {
            String originalUrl = urlDecode(state);
            responseBuilder = Response.temporaryRedirect(URI.create(originalUrl));
        } else {
            responseBuilder = Response.noContent();
        }
        responseBuilder.cookie(tokenCookie);
        responseBuilder.cookie(tokenExpirationCookie);
        responseBuilder.cookie(refreshTokenCookie);
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
