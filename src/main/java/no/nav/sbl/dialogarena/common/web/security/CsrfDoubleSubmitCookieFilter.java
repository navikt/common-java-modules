package no.nav.sbl.dialogarena.common.web.security;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static java.util.Arrays.*;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

public class CsrfDoubleSubmitCookieFilter implements Filter {

    /**
     * Laget etter https://www.owasp.org/index.php/Cross-Site_Request_Forgery_%28CSRF%29_Prevention_Cheat_Sheet#Double_Submit_Cookie
     **/

    private static final String CSRF_COOKIE_NAVN = "NAV_CSRF_PROTECTION";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if ("GET".equals(request.getMethod())) {
            if (stream(request.getCookies()).noneMatch(cookie -> cookie.getName().equals(CSRF_COOKIE_NAVN))) {
                response.addCookie(createCsrfProtectionCookie(request));
            }
        } else if ("POST".equals(request.getMethod())) {
            if (request.getHeader(CSRF_COOKIE_NAVN).equals(navCsrfCookieVerdi(request))) {
                filterChain.doFilter(request, response);
            } else {
                response.sendError(SC_UNAUTHORIZED);
            }
        }
    }

    private String navCsrfCookieVerdi(HttpServletRequest request) {
        return stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(CSRF_COOKIE_NAVN))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Mangler NAV_CSRF_PROTECTION-cookie!!"))
                .getValue();
    }

    private Cookie createCsrfProtectionCookie(HttpServletRequest request) {
        Cookie cookie = new Cookie(CSRF_COOKIE_NAVN, UUID.randomUUID().toString());
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(-1);
        cookie.setDomain(request.getServerName());
        return cookie;
    }

    @Override
    public void destroy() {

    }
}
