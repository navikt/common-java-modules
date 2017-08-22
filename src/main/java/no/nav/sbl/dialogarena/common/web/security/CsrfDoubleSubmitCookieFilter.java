package no.nav.sbl.dialogarena.common.web.security;

import no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet;
import org.slf4j.Logger;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static java.util.Arrays.stream;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Laget etter https://www.owasp.org/index.php/Cross-Site_Request_Forgery_%28CSRF%29_Prevention_Cheat_Sheet#Double_Submit_Cookie
 **/
public class CsrfDoubleSubmitCookieFilter implements Filter {
    private static final Logger LOG = getLogger(SelfTestBaseServlet.class);
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
        } else if ("POST".equals(request.getMethod()) || "DELETE".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
            if (navCsrfCookieVerdi(request).equals(request.getHeader(CSRF_COOKIE_NAVN))) {
                filterChain.doFilter(request, response);
            } else {
                LOG.error("Feil i CSRF-sjekk. Bruker du dette filteren må du i frontend sørge for å sende med NAV_CSRF_PROTECTION-cookien som en header med navn NAV_CSRF_PROTECTION og verdien til cookien");
                response.sendError(SC_UNAUTHORIZED, "Mangler NAV_CSRF_PROTECTION-cookie!! Du må inkludere cookien-verdien i en header med navn NAV_CSRF_PROTECTION");
            }
        }
    }

    private String navCsrfCookieVerdi(HttpServletRequest request) {
        return stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(CSRF_COOKIE_NAVN))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Mangler NAV_CSRF_PROTECTION-cookie!! Du må inkludere cookien-verdien i en header med navn NAV_CSRF_PROTECTION"))
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
