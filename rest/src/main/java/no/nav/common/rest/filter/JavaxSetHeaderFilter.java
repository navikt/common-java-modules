package no.nav.common.rest.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiFunction;

@Deprecated
public class JavaxSetHeaderFilter implements Filter {

    private final BiFunction<HttpServletRequest, HttpServletResponse, Map<String, String>> createHeaders;

    public JavaxSetHeaderFilter(Map<String, String> headers) {
        this.createHeaders = (req, resp) -> headers;
    }

    public JavaxSetHeaderFilter(BiFunction<HttpServletRequest, HttpServletResponse, Map<String, String>> createHeaders) {
        this.createHeaders = createHeaders;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        createHeaders.apply(request, response).forEach(response::setHeader);

        chain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

}
