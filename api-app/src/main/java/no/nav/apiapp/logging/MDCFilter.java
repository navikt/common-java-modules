package no.nav.apiapp.logging;

import no.nav.common.auth.SubjectHandler;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static no.nav.apiapp.util.StringUtils.of;
import static no.nav.log.MDCConstants.*;
import static no.nav.sbl.rest.RestUtils.CORRELATION_ID_HEADER_NAME;


public class MDCFilter extends OncePerRequestFilter {

    public static final String CALL_ID_HEADER_NAME = "X-Call-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        String userId = SubjectHandler.getIdent().orElse("");
        String callId = generateId();
        String correlationId = of(httpServletRequest.getHeader(CORRELATION_ID_HEADER_NAME)).orElseGet(MDCFilter::generateId);

        MDC.put(MDC_CALL_ID, callId);
        MDC.put(MDC_USER_ID, userId);
        MDC.put(MDC_CORRELATION_ID, correlationId);

        httpServletResponse.addHeader(CALL_ID_HEADER_NAME, callId);
        httpServletResponse.addHeader(CORRELATION_ID_HEADER_NAME, correlationId);

        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } finally {
            MDC.remove(MDC_CALL_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CORRELATION_ID);
        }
    }

    private static String generateId() {
        UUID uuid = UUID.randomUUID();
        return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
    }

}
