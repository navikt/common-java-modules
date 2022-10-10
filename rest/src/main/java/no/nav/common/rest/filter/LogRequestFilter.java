package no.nav.common.rest.filter;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.log.MDCConstants;
import no.nav.common.utils.IdUtils;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static no.nav.common.utils.IdUtils.generateId;

@Slf4j
public class LogRequestFilter implements Filter {

    public static final String NAV_CONSUMER_ID_HEADER_NAME = "Nav-Consumer-Id";

    public static final String NAV_CALL_ID_HEADER_NAME = "Nav-Call-Id";

    private static final String LOG_FILTER_FILTERED = "LOG_FILTER_FILTERED";

    private static final String RANDOM_USER_ID_COOKIE_NAME = "RUIDC";

    private static final int ONE_MONTH_IN_SECONDS = 60 * 60 * 24 * 30;

    private final String applicationName;

    private final boolean exposeErrorDetails;

    public LogRequestFilter(String applicationName) {
        this.applicationName = applicationName;
        this.exposeErrorDetails = false;
    }

    public LogRequestFilter(String applicationName, boolean exposeErrorDetails) {
        this.applicationName = applicationName;
        this.exposeErrorDetails = exposeErrorDetails;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("LogFilter supports only HTTP requests");
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        boolean hasAlreadyFilteredAttribute = request.getAttribute(LOG_FILTER_FILTERED) != null;

        // Make sure that the same request does not get filtered twice
        if (hasAlreadyFilteredAttribute) {
            filterChain.doFilter(request, response);
        } else {
            request.setAttribute(LOG_FILTER_FILTERED, Boolean.TRUE);
            try {
                filter(httpRequest, httpResponse, filterChain);
            } finally {
                request.removeAttribute(LOG_FILTER_FILTERED);
            }
        }
    }

    public void filter(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain) throws IOException, ServletException {
        Optional<String> maybeUserId = resolveUserId(httpRequest);

        String userId;

        if (maybeUserId.isPresent()) {
            userId = maybeUserId.get();
        } else {
            userId = generateId();
            createUserIdCookie(userId, httpResponse);
        }

        String consumerId = httpRequest.getHeader(NAV_CONSUMER_ID_HEADER_NAME);
        String callId = resolveCallId(httpRequest);

        MDC.put(MDCConstants.MDC_CALL_ID, callId);
        MDC.put(MDCConstants.MDC_USER_ID, userId);
        MDC.put(MDCConstants.MDC_CONSUMER_ID, consumerId);
        MDC.put(MDCConstants.MDC_REQUEST_ID, generateId());

        httpResponse.setHeader(NAV_CALL_ID_HEADER_NAME, callId);

        if (applicationName != null) {
            httpResponse.setHeader("Server", applicationName);
        }

        try {
            filterWithErrorHandling(httpRequest, httpResponse, filterChain);

            if (!isInternalRequest(httpRequest)) {
                String msg = format("status=%s method=%s host=%s path=%s",
                        httpResponse.getStatus(),
                        httpRequest.getMethod(),
                        httpRequest.getServerName(),
                        httpRequest.getRequestURI()
                );

                log.info(msg);
            }
        } finally {
            MDC.remove(MDCConstants.MDC_CALL_ID);
            MDC.remove(MDCConstants.MDC_USER_ID);
            MDC.remove(MDCConstants.MDC_CONSUMER_ID);
            MDC.remove(MDCConstants.MDC_REQUEST_ID);
        }
    }

    public static boolean isInternalRequest(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getRequestURI().contains("/internal/");
    }

    public static String resolveCallId(HttpServletRequest httpRequest) {
        return ofNullable(httpRequest.getHeader(NAV_CALL_ID_HEADER_NAME))
                .filter(v -> !v.isBlank())
                .orElseGet(IdUtils::generateId);
    }

    private void createUserIdCookie(String userId, HttpServletResponse httpResponse) {
        Cookie cookie = new Cookie(RANDOM_USER_ID_COOKIE_NAME, userId);
        cookie.setPath("/");
        cookie.setMaxAge(ONE_MONTH_IN_SECONDS);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        httpResponse.addCookie(cookie);
    }

    private void filterWithErrorHandling(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            filterChain.doFilter(httpRequest, httpResponse);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (httpResponse.isCommitted()) {
                log.error("failed with status={}", httpResponse.getStatus());
                throw e;
            } else {
                httpResponse.setStatus(500);
                if (exposeErrorDetails) {
                    e.printStackTrace(httpResponse.getWriter());
                }
            }
        }
    }

    private Optional<String> resolveUserId(HttpServletRequest httpRequest) {
        return ofNullable(httpRequest.getCookies())
                .flatMap(cookies -> {
                    for (Cookie cookie : cookies) {
                        if (RANDOM_USER_ID_COOKIE_NAME.equals(cookie.getName())) {
                            return ofNullable(cookie.getValue());
                        }
                    }

                    return empty();
                });
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
