package no.nav.common.test.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;

/**
 * Brukes for å legge til AuthContext når man kjører lokalt
 */
@Deprecated
public class JavaxTestAuthContextFilter implements Filter {

    private final AuthContext authContext;

    public JavaxTestAuthContextFilter(AuthContext authContext) {
        this.authContext = authContext;
    }

    public JavaxTestAuthContextFilter(UserRole role, String subject) {
        this.authContext = AuthTestUtils.createAuthContext(role, subject);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        AuthContextHolderThreadLocal.instance().withContext(authContext, () -> filterChain.doFilter(servletRequest, servletResponse));
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

}
