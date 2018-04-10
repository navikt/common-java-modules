package no.nav.apiapp.logging;

import no.nav.apiapp.util.SubjectUtils;
import no.nav.log.MDCConstants;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;


public class MDCFilter extends OncePerRequestFilter {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        String userId = SubjectUtils.getUserId().orElse("");
        String consumerId = SubjectUtils.getConsumerId().orElse("");
        String callId = generateCallId();

        MDC.put(MDCConstants.MDC_CALL_ID, callId);
        MDC.put(MDCConstants.MDC_USER_ID, userId);
        MDC.put(MDCConstants.MDC_CONSUMER_ID, consumerId);

        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } finally {
            MDC.remove(MDCConstants.MDC_CALL_ID);
            MDC.remove(MDCConstants.MDC_USER_ID);
            MDC.remove(MDCConstants.MDC_CONSUMER_ID);
        }
    }

    private static String generateCallId() {
        StringBuilder callId = new StringBuilder();
        callId.append("CallId_");
        callId.append(System.currentTimeMillis());
        callId.append("_");
        callId.append(RANDOM.nextInt(Integer.MAX_VALUE));
        return callId.toString();
    }

}
