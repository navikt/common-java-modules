package no.nav.brukerdialog.security.jaspic;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static no.nav.brukerdialog.security.Constants.ID_TOKEN_COOKIE_NAME;
import static no.nav.brukerdialog.security.Constants.REFRESH_TOKEN_COOKIE_NAME;


class TokenLocator {

    public Optional<String> getToken(HttpServletRequest request) {
        Optional<String> tokenFromCookie = getCookie(request, ID_TOKEN_COOKIE_NAME);
        if (tokenFromCookie.isPresent()) {
            return tokenFromCookie;
        }
        return getTokenFromHeader(request);
    }

    public Optional<String> getRefreshToken(HttpServletRequest request) {
        return getCookie(request, REFRESH_TOKEN_COOKIE_NAME);
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

    private Optional<String> getTokenFromHeader(HttpServletRequest request) {
        String headerValue = request.getHeader("Authorization");
        return headerValue != null && !headerValue.isEmpty() && headerValue.startsWith("Bearer ")
                ? Optional.of(headerValue.substring("Bearer ".length()))
                : Optional.empty();
    }

}
