package no.nav.common.auth.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class CookieTokenFinder implements TokenFinder {

    private final String cookieName;

    public CookieTokenFinder(String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public Optional<String> findToken(HttpServletRequest request) {
        return CookieUtils.getCookie(cookieName, request)
                .map(Cookie::getValue);
    }

}
