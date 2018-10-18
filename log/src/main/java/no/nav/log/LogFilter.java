package no.nav.log;

import lombok.extern.slf4j.Slf4j;
import no.nav.sbl.util.StringUtils;
import org.slf4j.MDC;
import org.springframework.context.EnvironmentAware;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;

import static no.nav.log.MDCConstants.*;
import static no.nav.sbl.util.LogUtils.buildMarker;
import static no.nav.sbl.util.StringUtils.nullOrEmpty;
import static no.nav.sbl.util.StringUtils.of;


@Slf4j
public class LogFilter extends OncePerRequestFilter implements EnvironmentAware {


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


    /**
     * Filter init param used to specify a {@link Supplier<Boolean>} that will return whether stacktraces should be exposed or not
     * Defaults to always false
     */
    private Supplier<Boolean> exposeErrorDetails;

    public LogFilter() {
        this(() -> false);
    }

    public LogFilter(Supplier<Boolean> exposeErrorDetails) {
        this.exposeErrorDetails = exposeErrorDetails;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        String userId = resolveUserId(httpServletRequest);
        if (nullOrEmpty(userId)) {
            // user-id tracking only works if the client is stateful and supports cookies.
            // if no user-id is found, generate one for any following requests but do not use it on the current request to avoid generating large numbers of useless user-ids.
            generateUserIdCookie(httpServletResponse);
        }

        String consumerId = of(httpServletRequest.getHeader(CONSUMER_ID_HEADER_NAME)).orElseGet(LogFilter::generateId);
        String callId = Arrays.stream(NAV_CALL_ID_HEADER_NAMES).map(httpServletRequest::getHeader)
                .filter(StringUtils::notNullOrEmpty)
                .findFirst()
                .orElseGet(LogFilter::generateId);

        MDC.put(MDC_CALL_ID, callId);
        MDC.put(MDC_USER_ID, userId);
        MDC.put(MDC_CONSUMER_ID, consumerId);
        MDC.put(MDC_REQUEST_ID, generateId());

        httpServletResponse.addHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, callId);

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
            MDC.remove(MDC_CONSUMER_ID);
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private void generateUserIdCookie(HttpServletResponse httpServletResponse) {
        String userId = generateId();
        Cookie cookie = new Cookie(RANDOM_USER_ID_COOKIE_NAME, userId);
        cookie.setPath("/");
        cookie.setMaxAge(ONE_MONTH_IN_SECONDS);
        httpServletResponse.addCookie(cookie);
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
                if (exposeErrorDetails.get()) {
                    e.printStackTrace(httpServletResponse.getWriter());
                }
            }
        }
    }

    private String resolveUserId(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (RANDOM_USER_ID_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private static String generateId() {
        UUID uuid = UUID.randomUUID();
        return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
    }

}
