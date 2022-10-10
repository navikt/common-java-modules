package no.nav.common.log;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.IdUtils;
import no.nav.common.utils.StringUtils;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

import static java.lang.String.format;
import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.common.utils.StringUtils.nullOrEmpty;

@Slf4j
public class LogFilter implements Filter {

    private static final String LOG_FILTER_FILTERED = "LOG_FILTER_FILTERED";

    public static final String CONSUMER_ID_HEADER_NAME = "Nav-Consumer-Id";

    // there is no consensus in NAV about header-names for correlation ids, so we support 'em all!
    // https://nav-it.slack.com/archives/C9UQ16AH4/p1538488785000100
    public static final String PREFERRED_NAV_CALL_ID_HEADER_NAME = "Nav-Call-Id";
    public static final String[] NAV_CALL_ID_HEADER_NAMES = {
            PREFERRED_NAV_CALL_ID_HEADER_NAME,
            "Nav-CallId",
            "X-Correlation-Id"
    };

    private static final String RANDOM_USER_ID_COOKIE_NAME = "RUIDC";
    private static final int ONE_MONTH_IN_SECONDS = 60 * 60 * 24 * 30;

    private final String applicationName;
    private final boolean exposeErrorDetails;

    public LogFilter(String applicationName) {
        this.applicationName = applicationName;
        this.exposeErrorDetails = false;
    }

    public LogFilter(String applicationName, boolean exposeErrorDetails) {
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
        String userId = resolveUserId(httpRequest);
        if (nullOrEmpty(userId)) {
            // user-id tracking only works if the client is stateful and supports cookies.
            // if no user-id is found, generate one for any following requests but do not use it on the current request to avoid generating large numbers of useless user-ids.
            generateUserIdCookie(httpResponse);
        }

        String consumerId = httpRequest.getHeader(CONSUMER_ID_HEADER_NAME);
        String callId = resolveCallId(httpRequest);

        MDC.put(MDCConstants.MDC_CALL_ID, callId);
        MDC.put(MDCConstants.MDC_USER_ID, userId);
        MDC.put(MDCConstants.MDC_CONSUMER_ID, consumerId);
        MDC.put(MDCConstants.MDC_REQUEST_ID, generateId());

        httpResponse.setHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, callId);

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
        return Arrays.stream(NAV_CALL_ID_HEADER_NAMES).map(httpRequest::getHeader)
                .filter(StringUtils::notNullOrEmpty)
                .findFirst()
                .orElseGet(IdUtils::generateId);
    }

    private void generateUserIdCookie(HttpServletResponse httpResponse) {
        String userId = generateId();
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

    public String resolveUserId(HttpServletRequest httpRequest) {
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (RANDOM_USER_ID_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}
