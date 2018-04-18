package no.nav.brukerdialog.security.jaspic;

import no.nav.brukerdialog.security.domain.AuthenticationLevelCredential;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.domain.SluttBruker;
import no.nav.brukerdialog.security.oidc.OidcTokenValidator;
import no.nav.brukerdialog.security.oidc.OidcTokenValidatorResult;
import no.nav.brukerdialog.security.oidc.provider.OidcProvider;
import no.nav.brukerdialog.tools.HostUtils;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.security.auth.message.AuthStatus.*;
import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.brukerdialog.security.oidc.TokenUtils.getOpenamClientFromToken;
import static no.nav.brukerdialog.tools.Utils.getRelativePath;

/**
 * Stjålet mye fra https://github.com/omnifaces/omnisecurity
 */
public class OidcAuthModule implements ServerAuthModule {
    private static final Logger log = LoggerFactory.getLogger(OidcAuthModule.class);
    private static final Class<?>[] supportedMessageTypes = new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    // Key in the MessageInfo Map that when present AND set to true indicated a protected resource is being accessed.
    // When the resource is not protected, GlassFish omits the key altogether. WebSphere does insert the key and sets
    // it to false.
    private static final String IS_MANDATORY = "javax.security.auth.message.MessagePolicy.isMandatory";

    private static final boolean sslOnlyCookies = !Boolean.valueOf(System.getProperty("develop-local", "false"));

    private final List<OidcProvider> providers;
    private final OidcTokenValidator oidcTokenValidator = new OidcTokenValidator();
    private final boolean statelessApplication;
    private CallbackHandler handler;

    public OidcAuthModule(List<OidcProvider> providers, boolean statelessApplication) {
        this.providers = providers;
        this.statelessApplication = statelessApplication;
    }

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
        try {
            return doValidateRequest(messageInfo, clientSubject);
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
            throw t;
        }
    }

    private AuthStatus doValidateRequest(MessageInfo messageInfo, Subject clientSubject) {
        if (!isProtected(messageInfo)) {
            ensureStatelessApplication(messageInfo);
            return handleUnprotectedResource(clientSubject);
        }

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        return providers.stream()
                .filter(oidcProvider -> oidcProvider.match(request))
                .findFirst()
                .map(oidcProvider -> doValidateRequest(messageInfo, clientSubject, oidcProvider))
                .orElseGet(() -> responseUnAuthorized(messageInfo));
    }

    private AuthStatus doValidateRequest(MessageInfo messageInfo, Subject clientSubject, OidcProvider oidcProvider) {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        Optional<String> optionalRequestToken = oidcProvider.getToken(request);
        if (!optionalRequestToken.isPresent()) {
            return responseUnAuthorized(messageInfo);
        }

        String requestToken = optionalRequestToken.get();
        OidcTokenValidatorResult requestTokenValidatorResult = oidcTokenValidator.validate(requestToken, oidcProvider);
        Optional<String> optionalRefreshToken = oidcProvider.getRefreshToken(request);
        if (optionalRefreshToken.isPresent() && needToRefreshToken(requestTokenValidatorResult)) {
            String refreshToken = optionalRefreshToken.get();
            Optional<String> optionalRefreshedToken = fetchUpdatedToken(refreshToken, requestToken, oidcProvider);
            if (optionalRefreshedToken.isPresent()) {
                String refreshedToken = optionalRefreshedToken.get();
                OidcTokenValidatorResult refreshedTokenValidatorResult = oidcTokenValidator.validate(refreshedToken,oidcProvider);
                if (refreshedTokenValidatorResult.isValid()) {
                    registerUpdatedTokenAtUserAgent(messageInfo, refreshedToken);
                    ensureStatelessApplication(messageInfo);
                    return handleValidatedToken(refreshedToken, clientSubject, refreshedTokenValidatorResult.getSubject());
                }
            }
        }

        if (requestTokenValidatorResult.isValid()) {
            ensureStatelessApplication(messageInfo);
            return handleValidatedToken(requestToken, clientSubject, requestTokenValidatorResult.getSubject());
        }
        return responseUnAuthorized(messageInfo);
    }

    /**
     * Wrapps the request in a object that throws an {@link IllegalArgumentException} when invoking getSession og getSession(true)
     *
     * @throws IllegalArgumentException
     */
    private void ensureStatelessApplication(MessageInfo messageInfo) {
        if (this.statelessApplication) {
            messageInfo.setRequestMessage(new StatelessHttpServletRequest((HttpServletRequest) messageInfo.getRequestMessage()));
        }
    }

    private boolean needToRefreshToken(OidcTokenValidatorResult validateResult) {
        return !validateResult.isValid() || tokenIsSoonExpired(validateResult);
    }

    private boolean tokenIsSoonExpired(OidcTokenValidatorResult validateResult) {
        return validateResult.getExpSeconds() * 1000 - Instant.now().toEpochMilli() < getMinimumTimeToExpireBeforeRefresh();
    }

    private void registerUpdatedTokenAtUserAgent(MessageInfo messageInfo, String udatedIdToken) {
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        addHttpOnlyCookie(request, response, ID_TOKEN_COOKIE_NAME, udatedIdToken);
    }

    private int getMinimumTimeToExpireBeforeRefresh() {
        return Integer.parseInt(System.getProperty(REFRESH_TIME, "60")) * 1000;
    }

    private Optional<String> fetchUpdatedToken(String refreshToken, String requestToken, OidcProvider oidcProvider) {
        log.debug("Refreshing token"); //Do not log token
        try {
            return of(oidcProvider.getFreshToken(refreshToken, requestToken).getToken());
        } catch (Exception e) {
            log.error("Could not refresh token", e);
            return empty();
        }
    }

    private void addHttpOnlyCookie(HttpServletRequest request, HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setSecure(sslOnlyCookies);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        String domain = HostUtils.cookieDomain(request);
        if (domain != null) { //null for localhost
            cookie.setDomain(domain);
        }
        response.addCookie(cookie);
    }

    private void addApplicationCallbackSpecificHttpOnlyCookie(HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setSecure(sslOnlyCookies);
        cookie.setHttpOnly(true);
        cookie.setPath(getRelativePath(getOidcRedirectUrl()));
        //to work on app.adeo.no and modapp.adeo.no
        cookie.setDomain(".adeo.no");
        response.addCookie(cookie);
    }

    private AuthStatus responseUnAuthorized(MessageInfo messageInfo) {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
        try {
            if ("application/json".equals(request.getHeader("Accept")) || !hasRedirectUrl()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Resource is protected, but id token is missing or invalid.");
            } else {
                AuthorizationRequestBuilder builder = new AuthorizationRequestBuilder();
                //TODO CSRF attack protection. See RFC-6749 section 10.12 (the state-cookie containing redirectURL shold be encrypted to avoid tampering)
                addApplicationCallbackSpecificHttpOnlyCookie(response, builder.getStateIndex(), encode(getOriginalUrl(request)));
                response.sendRedirect(builder.buildRedirectString());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return SEND_FAILURE;
    }


    private String encode(String redirectLocation) throws UnsupportedEncodingException {
        return URLEncoder.encode(redirectLocation, "UTF-8");
    }

    private String getOriginalUrl(HttpServletRequest req) {
        return req.getQueryString() == null
                ? req.getRequestURL().toString()
                : req.getRequestURL().toString() + "?" + req.getQueryString();
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
        if (username != null && !username.isEmpty()) {
            clientSubject.getPrincipals().add(SluttBruker.internBruker(username));
            clientSubject.getPublicCredentials().add(new AuthenticationLevelCredential(4));
            clientSubject.getPublicCredentials().add(new OidcCredential(token));
        }
        try {
            handler.handle(new Callback[]{new CallerPrincipalCallback(clientSubject, username)});
        } catch (IOException | UnsupportedCallbackException e) {
            // Should not happen
            throw new IllegalStateException(e);
        }
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
