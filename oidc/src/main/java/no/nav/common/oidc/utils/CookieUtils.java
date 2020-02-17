package no.nav.common.oidc.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;
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

    public static Cookie createCookie(String name, String value, String domain, int maxAge, boolean secure) {
        Cookie newCookie = new Cookie(name, value);
        newCookie.setDomain(domain);
        newCookie.setHttpOnly(true);
        newCookie.setSecure(secure);
        newCookie.setMaxAge(maxAge);
        return newCookie;
    }

    public static Cookie createCookie(String name, String value, Date expireAt, HttpServletRequest request) {
        return createCookie(name, value, cookieDomain(request), dateToCookieMaxAge(expireAt), request.isSecure());
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
