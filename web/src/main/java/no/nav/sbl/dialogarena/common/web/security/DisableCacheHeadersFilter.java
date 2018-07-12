package no.nav.sbl.dialogarena.common.web.security;

import lombok.Builder;
import lombok.Value;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DisableCacheHeadersFilter implements Filter {

    private final boolean allowClientStorage;

    @SuppressWarnings("unused")
    public DisableCacheHeadersFilter() {
        this(Config.builder().build());
    }

    public DisableCacheHeadersFilter(Config config) {
        this.allowClientStorage = config.allowClientStorage;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        if (allowClientStorage) {
            response.setHeader("cache-control", "no-cache");
        } else {
            response.setHeader("cache-control", "no-cache, no-store, must-revalidate");
        }
        response.setHeader("pragma", "no-cache");
        response.setHeader("expires", "0");
        filterChain.doFilter(servletRequest, response);
    }

    @Override
    public void destroy() {}

    @Builder
    @Value
    public static class Config {
        private boolean allowClientStorage;
    }

}
