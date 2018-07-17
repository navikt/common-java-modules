package no.nav.apiapp.logging;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.feil.FeilMapper;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static no.nav.apiapp.util.StringUtils.of;
import static no.nav.log.MDCConstants.*;
import static no.nav.sbl.rest.RestUtils.CORRELATION_ID_HEADER_NAME;


@Slf4j
public class LogFilter extends OncePerRequestFilter {

    public static final String CALL_ID_HEADER_NAME = "X-Call-Id";

    private static final String RANDOM_USER_ID_COOKIE_NAME = "RUIDC";
    private static final int ONE_MONTH_IN_SECONDS = 60 * 60 * 24 * 30;

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
            log.info("status={} method={} path={}",
                    httpServletResponse.getStatus(),
                    httpServletRequest.getMethod(),
                    httpServletRequest.getRequestURI()
            );
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
                if (FeilMapper.visDetaljer()) {
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
        return userId;
    }

    private static String generateId() {
        UUID uuid = UUID.randomUUID();
        return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
    }

}
