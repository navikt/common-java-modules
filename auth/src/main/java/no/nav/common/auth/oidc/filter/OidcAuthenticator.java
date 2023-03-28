package no.nav.common.auth.oidc.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Value;
import no.nav.common.auth.oidc.OidcTokenValidator;
import no.nav.common.auth.utils.CookieUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
public class OidcAuthenticator {

    public OidcTokenValidator tokenValidator;

    public OidcAuthenticatorConfig config;

    public static OidcAuthenticator fromConfig(OidcAuthenticatorConfig config) {
        if (!config.isValid()) {
            throw new IllegalStateException("OidcAuthenticatorConfig is missing one or more values");
        }

        OidcTokenValidator validator = new OidcTokenValidator(config.discoveryUrl, config.clientIds);
        return new OidcAuthenticator(validator, config);
    }

    public static List<OidcAuthenticator> fromConfigs(OidcAuthenticatorConfig ...configs) {
        return Arrays.stream(configs)
                .map(OidcAuthenticator::fromConfig)
                .collect(Collectors.toList());
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
