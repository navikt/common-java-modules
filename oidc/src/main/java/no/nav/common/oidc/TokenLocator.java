package no.nav.common.oidc;

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

    public Optional<String> getToken(HttpServletRequest request) {
        Optional<String> tokenFromCookie = getCookie(request, idTokenCookieName);
        if (tokenFromCookie.isPresent()) {
            return tokenFromCookie;
        }
        return getTokenFromHeader(request);
    }

    public Optional<String> getRefreshToken(HttpServletRequest request) {
        return getCookie(request, refreshTokenCookieName);
    }

    private Optional<String> getCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (javax.servlet.http.Cookie c : request.getCookies()) {
            if (c.getName().equals(cookieName) && c.getValue() != null) {
                return Optional.of(c.getValue());
            }
        }
        return Optional.empty();
    }

    public static Optional<String> getTokenFromHeader(HttpServletRequest request) {
        String headerValue = request.getHeader("Authorization");
        return headerValue != null && !headerValue.isEmpty() && headerValue.startsWith("Bearer ")
                ? Optional.of(headerValue.substring("Bearer ".length()))
                : Optional.empty();
    }

}
