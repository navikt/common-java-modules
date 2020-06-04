package no.nav.common.abac;

import lombok.Builder;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;

import static no.nav.common.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.common.log.LogFilter.resolveCallId;

@Builder
@Getter
public class AuditRequestInfo {
    private String callId;
    private String consumerId;
    private String requestMethod;
    private String requestPath;

    public static AuditRequestInfo fraHttpServletRequest(HttpServletRequest request) {
        return AuditRequestInfo.builder()
                .callId(resolveCallId(request))
                .consumerId(request.getHeader(CONSUMER_ID_HEADER_NAME))
                .requestMethod(request.getMethod())
                .requestPath(request.getRequestURI())
                .build();
    }
}
