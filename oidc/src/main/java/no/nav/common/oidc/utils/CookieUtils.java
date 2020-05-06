package no.nav.common.oidc.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;
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

    public static Cookie createUpdatedCookie(Cookie existingCookie, String value, Date expireAt) {
        Cookie newCookie = new Cookie(existingCookie.getName(), value);
        
        if (existingCookie.getDomain() != null) {
            newCookie.setDomain(existingCookie.getDomain());
        }
        newCookie.setPath(existingCookie.getPath());
        newCookie.setHttpOnly(true);
        newCookie.setSecure(true);
        newCookie.setMaxAge(dateToCookieMaxAge(expireAt));

        return newCookie;
    }

    public static int dateToCookieMaxAge(Date date) {
        long deltaTime = date.getTime() - System.currentTimeMillis();
        return (int) deltaTime / 1000;
    }

}
