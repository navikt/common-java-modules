package no.nav.common.web;

import org.slf4j.Logger;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Laget etter https://www.owasp.org/index.php/Cross-Site_Request_Forgery_%28CSRF%29_Prevention_Cheat_Sheet#Double_Submit_Cookie
 **/
public class CsrfDoubleSubmitCookieFilter implements Filter {

    public static final String CSRF_COOKIE_NAVN = "NAV_CSRF_PROTECTION";

    private static final Logger LOG = getLogger(CsrfDoubleSubmitCookieFilter.class);

    public static final String IGNORED_URLS_INIT_PARAMETER_NAME = "ignoredUrls";

    private static final Set<String> ALLOWED_METHODS = new HashSet<>(asList(
            "GET",
            "HEAD",
            "OPTIONS"
    ));

    private String[] ignoredUrls;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String param = filterConfig.getInitParameter(IGNORED_URLS_INIT_PARAMETER_NAME);
        ignoredUrls = (param != null) ? param.split(",") : new String[] {};
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (stream(ignoredUrls).noneMatch(path::startsWith)) {
            if (ALLOWED_METHODS.contains(request.getMethod())) {
                if (request.getCookies() == null || stream(request.getCookies()).noneMatch(cookie -> cookie.getName().equals(CSRF_COOKIE_NAVN))) {
                    response.addCookie(createCsrfProtectionCookie(request));
                }
            } else if (!cookieMatcherHeader(request)) {
                LOG.warn("Feil i CSRF-sjekk. " +
                        "Bruker du dette filteret må du i frontend sørge for å sende med NAV_CSRF_PROTECTION-cookien som en header med navn NAV_CSRF_PROTECTION og verdien til cookien. " +
                        "Er headeren satt? " + isNotBlank(request.getHeader(CSRF_COOKIE_NAVN)));
                response.sendError(SC_UNAUTHORIZED, "Mangler NAV_CSRF_PROTECTION-cookie!! Du må inkludere cookie-verdien i en header med navn NAV_CSRF_PROTECTION");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean cookieMatcherHeader(HttpServletRequest request) {
        return streamNullSafe(request.getCookies())
                .filter(cookie -> cookie.getName().equals(CSRF_COOKIE_NAVN))
                .map(Cookie::getValue)
                .findFirst()
                .map(v -> v.equals(request.getHeader(CSRF_COOKIE_NAVN)))
                .orElse(false);
    }

    private static <T> Stream<T> streamNullSafe(T[] cookies) {
        return cookies != null ? stream(cookies) : Stream.empty();
    }

    private Cookie createCsrfProtectionCookie(HttpServletRequest request) {
        Cookie cookie = new Cookie(CSRF_COOKIE_NAVN, UUID.randomUUID().toString());
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(3600 * 24 * 7);
        cookie.setDomain(request.getServerName());
        return cookie;
    }

    @Override
    public void destroy() {

    }
}
