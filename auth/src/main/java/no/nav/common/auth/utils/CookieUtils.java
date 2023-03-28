package no.nav.common.auth.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.UriInfo;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

public class CookieUtils {

    public static Optional<Cookie> getCookie(String cookieName, HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays
                        .stream(cookies)
                        .filter(cookie -> cookie.getName().equals(cookieName) && cookie.getValue() != null)
                        .findFirst()
                );
    }

    public static Optional<String> getCookieValue(String cookieName, HttpServletRequest request) {
        return getCookie(cookieName, request)
                .map(Cookie::getValue);
    }

    public static Cookie createCookie(String name, String value, String domain, String path, int maxAge, boolean secure) {
        Cookie newCookie = new Cookie(name, value);
        newCookie.setDomain(domain);
        newCookie.setPath(path);
        newCookie.setHttpOnly(true);
        newCookie.setSecure(secure);
        newCookie.setMaxAge(maxAge);
        return newCookie;
    }

    public static Cookie createCookie(String name, String value, Date expireAt, HttpServletRequest request) {
        return createCookie(name, value, cookieDomain(request), "/", dateToCookieMaxAge(expireAt), request.isSecure());
    }

    public static String cookieDomain(HttpServletRequest request) {
        String serverName = request.getServerName();
        return extractCookieDomain(serverName);
    }

    public static String cookieDomain(UriInfo uri) {
        String host = uri.getBaseUri().getHost();
        return extractCookieDomain(host);
    }

    public static String extractCookieDomain(String host) {
        return host.substring(host.indexOf('.') + 1);
    }

    public static int dateToCookieMaxAge(Date date) {
        long deltaTime = date.getTime() - System.currentTimeMillis();
        return (int) deltaTime / 1000;
    }

}
