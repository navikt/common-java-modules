package no.nav.common.auth.oidc.filter;

import lombok.Value;
import no.nav.common.auth.oidc.OidcTokenValidator;
import no.nav.common.auth.utils.CookieUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Value
public class OidcAuthenticator {

    public OidcTokenValidator tokenValidator;

    public OidcAuthenticatorConfig config;

    public static OidcAuthenticator fromConfig(OidcAuthenticatorConfig config) {
        if (!config.isValid()) {
            throw new IllegalStateException("OidcAuthenticatorConfig is missing one or more values");
        }

        OidcTokenValidator validator = new OidcTokenValidator(config.discoveryUrl, config.clientId);
        return new OidcAuthenticator(validator, config);
    }

    public Optional<String> findIdToken(HttpServletRequest request) {
        Optional<String> maybeIdTokenFromCookie = Optional.ofNullable(config.idTokenCookieName)
                .flatMap(tokenName -> CookieUtils.getCookieValue(tokenName, request));

        if (maybeIdTokenFromCookie.isPresent()) {
            return maybeIdTokenFromCookie;
        }

        return config.idTokenFinder.findToken(request);
    }

    public Optional<String> findRefreshToken(HttpServletRequest request) {
        return Optional.ofNullable(config.refreshTokenCookieName)
                .flatMap(tokenName -> CookieUtils.getCookieValue(tokenName, request));
    }

}
