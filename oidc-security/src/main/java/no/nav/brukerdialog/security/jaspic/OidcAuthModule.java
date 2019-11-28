package no.nav.brukerdialog.security.jaspic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukerdialog.security.jwks.CacheMissAction;
import no.nav.brukerdialog.security.oidc.OidcTokenValidator;
import no.nav.brukerdialog.security.oidc.OidcTokenValidatorResult;
import no.nav.brukerdialog.security.oidc.provider.OidcProvider;
import no.nav.brukerdialog.tools.HostUtils;
import no.nav.common.auth.LoginProvider;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Optional.*;
import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.brukerdialog.security.jwks.CacheMissAction.NO_REFRESH;
import static no.nav.brukerdialog.security.jwks.CacheMissAction.REFRESH;
import static no.nav.brukerdialog.tools.Utils.getRelativePath;

@Slf4j
public class OidcAuthModule implements LoginProvider {

    private static final Logger log = LoggerFactory.getLogger(OidcAuthModule.class);
    private static final boolean sslOnlyCookies = !Boolean.valueOf(System.getProperty("develop-local", "false"));
    public static final String AZURE_AD_CLAIM = "NAVident";

    private final List<OidcProvider> providers;
    private final OidcTokenValidator oidcTokenValidator;

    public OidcAuthModule(List<OidcProvider> providers) {
        this(providers, new OidcTokenValidator());
    }

    OidcAuthModule(List<OidcProvider> providers, OidcTokenValidator oidcTokenValidator) {
        this.providers = providers;
        this.oidcTokenValidator = oidcTokenValidator;
    }

    @Override
    public Optional<Subject> authenticate(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return Stream.of(NO_REFRESH, REFRESH)
                .flatMap(cacheControl -> authenticate(httpServletRequest, httpServletResponse, cacheControl))
                .findFirst();
    }

    private Stream<Subject> authenticate(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, CacheMissAction cacheMissAction) {
        return providers.stream().flatMap(catchAndLogErrors(oidcProviderHandler -> doValidateRequest(httpServletRequest, httpServletResponse, oidcProviderHandler, cacheMissAction)));
    }

    private Function<OidcProvider, Stream<Subject>> catchAndLogErrors(Function<OidcProvider, Stream<Subject>> providerStreamFunction) {
        return (oidcProvider) -> {
            try {
                return providerStreamFunction.apply(oidcProvider);
            } catch (Throwable t) {
                log.warn(t.getMessage(), t);
                return Stream.empty();
            }
        };
    }

    private Stream<Subject> doValidateRequest(
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse,
            OidcProvider oidcProvider,
            CacheMissAction cacheMissAction
    ) {
        Optional<String> optionalRequestToken = oidcProvider.getToken(httpServletRequest);
        if (!optionalRequestToken.isPresent()) {
            return Stream.empty();
        }

        String requestToken = optionalRequestToken.get();
        OidcTokenValidatorResult requestTokenValidatorResult = oidcTokenValidator.validate(requestToken, oidcProvider, cacheMissAction);
        Optional<String> optionalRefreshToken = oidcProvider.getRefreshToken(httpServletRequest);
        boolean needToRefreshToken = needToRefreshToken(requestTokenValidatorResult);
        if (optionalRefreshToken.isPresent() && needToRefreshToken) {
            String refreshToken = optionalRefreshToken.get();
            Optional<String> optionalRefreshedToken = fetchUpdatedToken(refreshToken, requestToken, oidcProvider);
            if (optionalRefreshedToken.isPresent()) {
                String refreshedToken = optionalRefreshedToken.get();
                OidcTokenValidatorResult refreshedTokenValidatorResult = oidcTokenValidator.validate(refreshedToken, oidcProvider, cacheMissAction);
                if (refreshedTokenValidatorResult.isValid()) {
                    addHttpOnlyCookie(httpServletRequest, httpServletResponse, ID_TOKEN_COOKIE_NAME, refreshedToken);
                    return handleValidatedToken(refreshedTokenValidatorResult, refreshedToken, oidcProvider);
                }
            }
        }

        if (requestTokenValidatorResult.isValid()) {
            return handleValidatedToken(requestTokenValidatorResult, requestToken, oidcProvider);
        }
        return Stream.empty();
    }

    private boolean needToRefreshToken(OidcTokenValidatorResult validateResult) {
        return !validateResult.isValid() || tokenIsSoonExpired(validateResult);
    }

    private boolean tokenIsSoonExpired(OidcTokenValidatorResult validateResult) {
        return validateResult.getExpSeconds() * 1000 - Instant.now().toEpochMilli() < getMinimumTimeToExpireBeforeRefresh();
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

    @Override
    @SneakyThrows
    public Optional<String> redirectUrl(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        if (hasRedirectUrl()) {
            AuthorizationRequestBuilder builder = new AuthorizationRequestBuilder();
            //TODO CSRF attack protection. See RFC-6749 section 10.12 (the state-cookie containing redirectURL shold be encrypted to avoid tampering)
            addApplicationCallbackSpecificHttpOnlyCookie(httpServletResponse, builder.getStateIndex(), encode(getOriginalUrl(httpServletRequest)));
            return of(builder.buildRedirectString());
        } else {
            return empty();
        }
    }

    private String encode(String redirectLocation) throws UnsupportedEncodingException {
        return URLEncoder.encode(redirectLocation, "UTF-8");
    }

    private String getOriginalUrl(HttpServletRequest req) {
        return req.getQueryString() == null
                ? req.getRequestURL().toString()
                : req.getRequestURL().toString() + "?" + req.getQueryString();
    }

    private Stream<Subject> handleValidatedToken(OidcTokenValidatorResult requestTokenValidatorResult, String token, OidcProvider oidcProvider) {
        SsoToken ssoToken = SsoToken.oidcToken(token, requestTokenValidatorResult.getAttributes());
        String username = ofNullable(requestTokenValidatorResult.getAttributes().get(AZURE_AD_CLAIM))
                .map((object) -> (String)object)
                .orElseGet(requestTokenValidatorResult::getSubject);

        Subject subject = new Subject(username, oidcProvider.getIdentType(token), ssoToken);
        return Stream.of(subject);
    }

}
