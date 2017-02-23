package no.nav.security.jwt.security.jaspic;

import no.nav.security.jwt.security.domain.AuthenticationLevelCredential;
import no.nav.security.jwt.security.domain.JwtCredential;
import no.nav.security.jwt.security.domain.SluttBruker;
import no.nav.security.jwt.security.jwks.JwksKeyHandler;
import no.nav.security.jwt.security.jwks.JwksKeyHandlers;
import no.nav.security.jwt.security.oidc.OidcTokenValidator;
import no.nav.security.jwt.security.oidc.TokenProvider;
import no.nav.security.jwt.tools.AuthorizationRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static javax.security.auth.message.AuthStatus.*;

/**
 * Stjålet mye fra https://github.com/omnifaces/omnisecurity
 */
public class JWTAuthModule implements ServerAuthModule {
    private static final Logger log = LoggerFactory.getLogger(JWTAuthModule.class);
    private static final Class<?>[] supportedMessageTypes = new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    // Key in the MessageInfo Map that when present AND set to true indicated a protected resource is being accessed.
    // When the resource is not protected, GlassFish omits the key altogether. WebSphere does insert the key and sets
    // it to false.
    private static final String IS_MANDATORY = "javax.security.auth.message.MessagePolicy.isMandatory";

    private static final JwksKeyHandler jwks = JwksKeyHandlers.createKeyHandler();
    private static final boolean sslOnlyCookies = !Boolean.valueOf(System.getProperty("develop-local", "false"));
    public static final String REFRESH_TIME = "no.nav.kes.security.oidc.minimum_time_to_expire_before_refresh.seconds";

    private CallbackHandler handler;

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options) throws AuthException {
        this.handler = handler;
    }

    @Override
    public Class[] getSupportedMessageTypes() {
        return supportedMessageTypes;
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        if (!isProtected(messageInfo)) {
            return handleUnprotectedResource(clientSubject);
        }
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        Optional<String> token = TokenLocator.getToken(request);
        Optional<String> refreshToken = TokenLocator.getRefreshToken(request);
        if (!token.isPresent()) {
            return responseUnAuthorized(messageInfo);
        }

        OidcTokenValidator validator = new OidcTokenValidator(jwks, token.get());
        if (validator.tokenIsValid()) {
            boolean shouldDoPreemptiveRefresh = refreshToken.isPresent() && (validator.getExp() - Instant.now().toEpochMilli() < getMinimumTimeToExpireBeforeRefresh());
            if (!shouldDoPreemptiveRefresh) {
                return handleValidatedToken(token.get(), clientSubject, validator.getSubject());
            }
        }
        if (!refreshToken.isPresent()) {
            return responseUnAuthorized(messageInfo);
        }
        Optional<JwtCredential> updatedToken = fetchUpdatedToken(refreshToken.get());
        if (updatedToken.isPresent()) {
            String udatedIdToken = updatedToken.get().getToken();
            validator = new OidcTokenValidator(jwks, udatedIdToken);
            if (validator.tokenIsValid()) {
                registerUpdatedTokensAtUserAgent(messageInfo, udatedIdToken);
                return handleValidatedToken(udatedIdToken, clientSubject, validator.getSubject());
            }
        }
        return responseUnAuthorized(messageInfo);
    }

    private void registerUpdatedTokensAtUserAgent(MessageInfo messageInfo, String udatedIdToken) {
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
        addHttpOnlyCookie(response, "Authentication", udatedIdToken);
    }

    private int getMinimumTimeToExpireBeforeRefresh() {
        return Integer.parseInt(System.getProperty(REFRESH_TIME, "60")) * 1000;
    }

    Optional<JwtCredential> fetchUpdatedToken(String refreshToken) {
        log.info("Refresing token using refresh token " + refreshToken);
        try {
            TokenProvider<JwtCredential> tokenProvider = TokenProvider.fromRefreshToken(refreshToken);
            return Optional.of(tokenProvider.getToken());
        } catch (Exception e) {
            log.error("Could not refresh token", e);
            return Optional.empty();
        }
    }

    private void addHttpOnlyCookie(HttpServletResponse response, String name, String value) {
        String contextRoot = "/"+System.getProperty("applicationName");
        Cookie cookie = new Cookie(name, value);
        cookie.setSecure(sslOnlyCookies);
        cookie.setHttpOnly(true);
        cookie.setPath(contextRoot);
        response.addCookie(cookie);
    }

    private AuthStatus responseUnAuthorized(MessageInfo messageInfo) {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
        String applicationName = System.getProperty("applicationName");
        String applicationRootUrl = "/"+applicationName+"/";
        String indexUrl = applicationRootUrl+"index.html";
        try {
            if(request.getRequestURI().equals(applicationRootUrl) ||request.getRequestURI().equals(indexUrl)){
                response.sendRedirect(new AuthorizationRequestBuilder(request)
                        .medRedirectToOriginalLocation()
                        .buildRedirectString());
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No token and protected resource. Will later implement redirect to OP");
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return SEND_FAILURE;
    }

    private AuthStatus handleUnprotectedResource(Subject clientSubject) {
        notifyContainerAboutLogin(clientSubject, handler, null, null);
        return SUCCESS;
    }

    private AuthStatus handleValidatedToken(String token, Subject clientSubject, String username) {
        notifyContainerAboutLogin(clientSubject, handler, username, token);
        return SUCCESS;
    }


    private void notifyContainerAboutLogin(Subject clientSubject, CallbackHandler handler, String username, String token) {
        if (!isEmpty(username)) {
            clientSubject.getPrincipals().add(SluttBruker.internBruker(username));
            clientSubject.getPublicCredentials().add(new AuthenticationLevelCredential(4));
            clientSubject.getPublicCredentials().add(new JwtCredential(token));
        }
        try {
            handler.handle(new Callback[]{new CallerPrincipalCallback(clientSubject, username)});
        } catch (IOException | UnsupportedCallbackException e) {
            // Should not happen
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns true if the given string is null or is empty.
     *
     * @param string The string to be checked on emptiness.
     * @return True if the given string is null or is empty.
     */
    public static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /**
     * Returns <code>true</code> if the given array is null or is empty.
     *
     * @param array The array to be checked on emptiness.
     * @return <code>true</code> if the given array is null or is empty.
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Returns <code>true</code> if the given collection is null or is empty.
     *
     * @param collection The collection to be checked on emptiness.
     * @return <code>true</code> if the given collection is null or is empty.
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    private boolean isProtected(MessageInfo messageInfo) {
        return Boolean.valueOf((String) messageInfo.getMap().get(IS_MANDATORY));
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return SEND_SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        if (subject != null) {
            subject.getPrincipals().clear();
        }
    }
}
