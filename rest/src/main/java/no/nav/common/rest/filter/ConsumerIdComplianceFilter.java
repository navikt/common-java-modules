package no.nav.common.rest.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

import static no.nav.common.rest.filter.LogRequestFilter.NAV_CONSUMER_ID_HEADER_NAME;

@Slf4j
public class ConsumerIdComplianceFilter implements Filter {

    private final boolean enforceCompliance;

    public ConsumerIdComplianceFilter(boolean enforceCompliance) {
        this.enforceCompliance = enforceCompliance;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        boolean isMissingConsumerId = getHeader(request, NAV_CONSUMER_ID_HEADER_NAME).isEmpty();

        if (isMissingConsumerId) {
            log.warn("Request is missing consumer id, enforcingCompliance={}", enforceCompliance);

            if (enforceCompliance) {
                HttpServletResponse response = (HttpServletResponse) servletResponse;

                response.setStatus(400);
                response.getWriter().write(
                        "Bad request: Consumer id is missing from header: " + NAV_CONSUMER_ID_HEADER_NAME +
                                ". Make sure to set the header with the name of the requesting application."
                );
                response.getWriter().flush();
                return;
            }
        }

        chain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

    private Optional<String> getHeader(HttpServletRequest request, String headerName) {
        return Optional.ofNullable(request.getHeader(headerName));
    }

}
