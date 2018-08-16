package no.nav.log;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.EnvironmentAware;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

import static no.nav.log.MDCConstants.*;
import static no.nav.sbl.util.LogUtils.buildMarker;
import static no.nav.sbl.util.StringUtils.of;


@Slf4j
public class LogFilter extends OncePerRequestFilter implements EnvironmentAware {

    /**
     * Filter init param used to specify a {@link Supplier<Boolean>} that will return whether stacktraces should be exposed or not
     * Defaults to always false
     */
    public static final String EXPOSE_DETAILS_SUPPLIER = "exposeDetailsSupplier";

    public static final String CALL_ID_HEADER_NAME = "X-Call-Id";
    public static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-Id";

    private static final String RANDOM_USER_ID_COOKIE_NAME = "RUIDC";
    private static final int ONE_MONTH_IN_SECONDS = 60 * 60 * 24 * 30;

    private Supplier<Boolean> exposeDetails;

    public LogFilter() {
        this(() -> false);
    }

    public LogFilter(Supplier<Boolean> exposeDetails) {
        this.exposeDetails = exposeDetails;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        String userId = resolveUserId(httpServletRequest, httpServletResponse);
        String callId = generateId();
        String correlationId = of(httpServletRequest.getHeader(CORRELATION_ID_HEADER_NAME)).orElseGet(LogFilter::generateId);

        MDC.put(MDC_CALL_ID, callId);
        MDC.put(MDC_USER_ID, userId);
        MDC.put(MDC_CORRELATION_ID, correlationId);

        httpServletResponse.addHeader(CALL_ID_HEADER_NAME, callId);
        httpServletResponse.addHeader(CORRELATION_ID_HEADER_NAME, correlationId);

        try {
            filterWithErrorHandling(httpServletRequest, httpServletResponse, filterChain);

            buildMarker()
                    .field("status", httpServletResponse.getStatus())
                    .field("method", httpServletRequest.getMethod())
                    .field("path", httpServletRequest.getRequestURI())
                    .log(log::info);

        } finally {
            MDC.remove(MDC_CALL_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CORRELATION_ID);
        }
    }

    private void filterWithErrorHandling(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (httpServletResponse.isCommitted()) {
                log.error("failed with status={}", httpServletResponse.getStatus());
                throw e;
            } else {
                httpServletResponse.setStatus(500);
                if (exposeDetails.get()) {
                    e.printStackTrace(httpServletResponse.getWriter());
                }
            }
        }
    }

    private String resolveUserId(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (RANDOM_USER_ID_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String userId = generateId();
        Cookie cookie = new Cookie(RANDOM_USER_ID_COOKIE_NAME, userId);
        cookie.setPath("/");
        cookie.setMaxAge(ONE_MONTH_IN_SECONDS);
        httpServletResponse.addCookie(cookie);

        // user-id tracking only works if the client is stateful and supports cookies.
        // return null to avoid stateless clients from generating large numbers of useless user-ids.
        return null;
    }

    private static String generateId() {
        UUID uuid = UUID.randomUUID();
        return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
    }

}
