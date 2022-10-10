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

    public static final String SERVER_HEADER_NAME = "Server";

    private static final String RANDOM_USER_ID_COOKIE_NAME = "RUIDC";

    private static final int ONE_MONTH_IN_SECONDS = 60 * 60 * 24 * 30;

    private final String applicationName;

    private final boolean exposeErrorDetails;

    public LogRequestFilter(String applicationName) {
       this(applicationName, false);
    }

    public LogRequestFilter(String applicationName, boolean exposeErrorDetails) {
        if (applicationName == null) {
            throw new IllegalArgumentException("Application name must not be null");
        }

        this.applicationName = applicationName;
        this.exposeErrorDetails = exposeErrorDetails;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("LogRequestFilter supports only HTTP requests");
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (isInternalRequest(httpRequest)) {
            filterChain.doFilter(httpRequest, httpResponse);
            return;
        }

        try {
            String consumerId = resolveConsumerId(httpRequest).orElse("unknown");
            String callId = resolveCallId(httpRequest).orElseGet(IdUtils::generateId);
            String userId = resolveUserId(httpRequest)
                    .orElseGet(() -> {
                        String newUserId = generateId();
                        createUserIdCookie(newUserId, httpResponse);
                        return newUserId;
                    });

            MDC.put(MDCConstants.MDC_CALL_ID, callId);
            MDC.put(MDCConstants.MDC_USER_ID, userId);
            MDC.put(MDCConstants.MDC_CONSUMER_ID, consumerId);
            MDC.put(MDCConstants.MDC_REQUEST_ID, generateId());

            httpResponse.setHeader(NAV_CALL_ID_HEADER_NAME, callId);
            httpResponse.setHeader(SERVER_HEADER_NAME, applicationName);

            filterChainWithErrorHandling(httpRequest, httpResponse, filterChain);

            String requestLogMsg = format("status=%s method=%s host=%s path=%s",
                    httpResponse.getStatus(),
                    httpRequest.getMethod(),
                    httpRequest.getServerName(),
                    httpRequest.getRequestURI()
            );

            log.info(requestLogMsg);
        } finally {
            MDC.remove(MDCConstants.MDC_CALL_ID);
            MDC.remove(MDCConstants.MDC_USER_ID);
            MDC.remove(MDCConstants.MDC_CONSUMER_ID);
            MDC.remove(MDCConstants.MDC_REQUEST_ID);
        }
    }

    private void filterChainWithErrorHandling(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Throwable e) {
            log.error("Uncaught exception", e);

            if (response.isCommitted()) {
                log.error("Response already committed, unable to set response error details");
            } else {
                response.setStatus(500);

                if (exposeErrorDetails) {
                    e.printStackTrace(response.getWriter());
                }
            }
        }
    }

    private void createUserIdCookie(String userId, HttpServletResponse httpResponse) {
        Cookie cookie = new Cookie(RANDOM_USER_ID_COOKIE_NAME, userId);
        cookie.setPath("/");
        cookie.setMaxAge(ONE_MONTH_IN_SECONDS);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        httpResponse.addCookie(cookie);
    }

    private static boolean isInternalRequest(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getRequestURI().contains("/internal/");
    }

    private static Optional<String> resolveConsumerId(HttpServletRequest httpRequest) {
        return ofNullable(httpRequest.getHeader(NAV_CONSUMER_ID_HEADER_NAME))
                .filter(v -> !v.isBlank());
    }

    private static Optional<String> resolveCallId(HttpServletRequest httpRequest) {
        return ofNullable(httpRequest.getHeader(NAV_CALL_ID_HEADER_NAME))
                .filter(v -> !v.isBlank());
    }

    private static Optional<String> resolveUserId(HttpServletRequest httpRequest) {
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
