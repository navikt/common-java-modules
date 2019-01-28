package no.nav.sbl.dialogarena.common.web.security;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class SecurityHeadersFilter implements Filter {

    private static final Pattern TJENESTER_NAV_NO_PATTERN = Pattern.compile("^tjenester(-q\\d)?\\.nav\\.no$");

    @Override
    public void init(FilterConfig filterConfig) { }

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        if (!skipAddingSecurityHeaders(httpServletRequest.getServerName())) {
            httpServletResponse.setHeader("X-Frame-Options", "DENY");
            httpServletResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpServletResponse.setHeader("X-XSS-Protection", "1; mode=block");
        }

        chain.doFilter(request, response);
    }

    static boolean skipAddingSecurityHeaders(String hostname) {
        // at "tjenester.nav.no", security headers are added by the loadbalancer.
        // duplication may cause conflict that can prevent security-features from being activated.
        return TJENESTER_NAV_NO_PATTERN.matcher(hostname).matches();
    }

    @Override
    public void destroy() { }

}
