package no.nav.common.oidc.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class TokenLocator {

    private final String idTokenCookieName;
    private final String refreshTokenCookieName;

    public TokenLocator(String idTokenCookieName) {
        this.idTokenCookieName = idTokenCookieName;
        this.refreshTokenCookieName = null;
    }

    public TokenLocator(String idTokenCookieName, String refreshTokenCookieName) {
        this.idTokenCookieName = idTokenCookieName;
        this.refreshTokenCookieName = refreshTokenCookieName;
    }

    public String getIdTokenCookieName() {
        return idTokenCookieName;
    }

    public String getRefreshTokenCookieName() {
        return refreshTokenCookieName;
    }

    public Optional<String> getIdToken(HttpServletRequest request) {
        Optional<String> tokenFromCookie = getIdTokenCookie(request).map(Cookie::getValue);

        if (tokenFromCookie.isPresent()) {
            return tokenFromCookie;
        }

        return getTokenFromHeader(request);
    }

    public Optional<String> getRefreshToken(HttpServletRequest request) {
        return getRefreshTokenCookie(request).map(Cookie::getValue);
    }

    public Optional<Cookie> getIdTokenCookie(HttpServletRequest request) {
        return CookieUtils.getCookie(idTokenCookieName, request);
    }

    public Optional<Cookie> getRefreshTokenCookie(HttpServletRequest request) {
        return CookieUtils.getCookie(refreshTokenCookieName, request);
    }

    public static Optional<String> getTokenFromHeader(HttpServletRequest request) {
        String headerValue = request.getHeader("Authorization");
        return headerValue != null && !headerValue.isEmpty() && headerValue.startsWith("Bearer ")
                ? Optional.of(headerValue.substring("Bearer ".length()))
                : Optional.empty();
    }

}
