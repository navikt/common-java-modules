package no.nav.sbl.dialogarena.common.web.filter;


import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GZIPFilter implements Filter {

    public static final String GZIP_FILTER_ATTRIBUTE_NAME = GZIPFilter.class.getName();

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (req instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;
            String ae = request.getHeader("accept-encoding");
            if (ae != null && ae.contains("gzip") && request.getAttribute(GZIP_FILTER_ATTRIBUTE_NAME) == null ) {
                request.setAttribute(GZIP_FILTER_ATTRIBUTE_NAME, Boolean.TRUE.toString());
                GZIPResponseWrapper wrappedResponse = new GZIPResponseWrapper(response);
                chain.doFilter(req, wrappedResponse);
                wrappedResponse.finish();
                return;
            }
            chain.doFilter(req, res);
        }
    }

    public void init(FilterConfig filterConfig) {
    }

    public void destroy() {
    }
}