package no.nav.security.jwt.security.jaspic;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

abstract class TokenLocator {

    static Optional<String> getToken(HttpServletRequest request) {
        Optional<String> tokenFromCookie = getTokenFromCookie(request);
        if (tokenFromCookie.isPresent()) {
            return tokenFromCookie;
        }
        return getTokenFromHeader(request);
    }

    static Optional<String> getRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (javax.servlet.http.Cookie c : request.getCookies()) {
            if (c.getName().equals("AuthenticationRefresh") && c.getValue() != null) {
                return Optional.of(c.getValue());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> getTokenFromHeader(HttpServletRequest request) {
        String headerValue = request.getHeader("Authorization");
        return headerValue != null && !headerValue.isEmpty() && headerValue.startsWith("Bearer ")
                ? Optional.of(headerValue.substring("Bearer ".length()))
                : Optional.empty();
    }

    private static Optional<String> getTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (javax.servlet.http.Cookie c : request.getCookies()) {
            if (c.getName().equals("Authentication") && c.getValue() != null) {
                return Optional.of(c.getValue());
            }
        }
        return Optional.empty();
    }
}
