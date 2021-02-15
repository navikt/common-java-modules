package no.nav.common.test.auth;

import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;

import javax.servlet.*;

/**
 * Brukes for å legge til AuthContext når man kjører lokalt
 */
public class TestAuthContextFilter implements Filter {

    private final AuthContext authContext;

    public TestAuthContextFilter(AuthContext authContext) {
        this.authContext = authContext;
    }

    public TestAuthContextFilter(UserRole role, String subject) {
        this.authContext = AuthTestUtils.createAuthContext(role, subject);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        AuthContextHolder.instance().withContext(authContext, () -> filterChain.doFilter(servletRequest, servletResponse));
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

}
