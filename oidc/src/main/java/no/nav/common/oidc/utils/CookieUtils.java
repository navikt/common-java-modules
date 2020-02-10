package no.nav.common.oidc.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Optional;

public class CookieUtils {

    public static Optional<Cookie> getCookie(String cookieName, HttpServletRequest request) {
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(cookieName) && cookie.getValue() != null) {
                return Optional.of(cookie);
            }
        }

        return Optional.empty();
    }

    public static Cookie fromCookie(Cookie cookie, String name, String value) {
        Cookie newCookie = new Cookie(name, value);
        newCookie.setDomain(cookie.getDomain());
        newCookie.setHttpOnly(cookie.isHttpOnly());
        newCookie.setSecure(cookie.getSecure());
        newCookie.setPath(cookie.getPath());
        newCookie.setMaxAge(cookie.getMaxAge());
        return newCookie;
    }

    public static int dateToCookieMaxAge(Date date) {
        long deltaTime = date.getTime() - System.currentTimeMillis();
        return (int) deltaTime / 1000;
    }

}
