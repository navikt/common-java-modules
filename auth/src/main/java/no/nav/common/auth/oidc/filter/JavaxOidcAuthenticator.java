package no.nav.common.auth.oidc.filter;

import lombok.Value;
import no.nav.common.auth.oidc.OidcTokenValidator;
import no.nav.common.auth.utils.JavaxCookieUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Deprecated
@Value
public class JavaxOidcAuthenticator {

    public OidcTokenValidator tokenValidator;

    public JavaxOidcAuthenticatorConfig config;

    public static JavaxOidcAuthenticator fromConfig(JavaxOidcAuthenticatorConfig config) {
        if (!config.isValid()) {
            throw new IllegalStateException("OidcAuthenticatorConfig is missing one or more values");
        }

        OidcTokenValidator validator = new OidcTokenValidator(config.discoveryUrl, config.clientIds);
        return new JavaxOidcAuthenticator(validator, config);
    }

    public static List<JavaxOidcAuthenticator> fromConfigs(JavaxOidcAuthenticatorConfig ...configs) {
        return Arrays.stream(configs)
                .map(JavaxOidcAuthenticator::fromConfig)
                .collect(Collectors.toList());
    }

    public Optional<String> findIdToken(HttpServletRequest request) {
        Optional<String> maybeIdTokenFromCookie = Optional.ofNullable(config.idTokenCookieName)
                .flatMap(tokenName -> JavaxCookieUtils.getCookieValue(tokenName, request));

        if (maybeIdTokenFromCookie.isPresent()) {
            return maybeIdTokenFromCookie;
        }

        return config.idTokenFinder.findToken(request);
    }

    public Optional<String> findRefreshToken(HttpServletRequest request) {
        return Optional.ofNullable(config.refreshTokenCookieName)
                .flatMap(tokenName -> JavaxCookieUtils.getCookieValue(tokenName, request));
    }

}
