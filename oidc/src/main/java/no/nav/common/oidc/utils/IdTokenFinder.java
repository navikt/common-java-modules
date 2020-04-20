package no.nav.common.oidc.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class IdTokenFinder implements TokenFinder {

    private final String idTokenCookieName;

    public IdTokenFinder(String idTokenCookieName) {
        this.idTokenCookieName = idTokenCookieName;
    }

    @Override
    public Optional<String> findToken(HttpServletRequest request) {
        return CookieUtils.getCookie(idTokenCookieName, request)
                .map(Cookie::getValue)
                .or(() -> TokenUtils.getTokenFromHeader(request));
    }

}
