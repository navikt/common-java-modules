package no.nav.common.abac;

import lombok.Builder;

import javax.servlet.http.HttpServletRequest;

import static no.nav.common.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.common.log.LogFilter.resolveCallId;

@Builder
public class RequestInfo {
    String callId;
    String consumerId;
    String requestMethod;
    String requestPath;

    public static RequestInfo fraHttpServletRequest(HttpServletRequest request) {
        return RequestInfo.builder()
                .callId(resolveCallId(request))
                .consumerId(request.getHeader(CONSUMER_ID_HEADER_NAME))
                .requestMethod(request.getMethod())
                .requestPath(request.getRequestURI())
                .build();
    }
}
