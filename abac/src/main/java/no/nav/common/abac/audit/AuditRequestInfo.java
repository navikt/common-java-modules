package no.nav.common.abac.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.servlet.http.HttpServletRequest;

import static no.nav.common.rest.filter.LogRequestFilter.*;

@Builder
@AllArgsConstructor
public class AuditRequestInfo {
    private String callId;
    private String consumerId;
    private String requestMethod;
    private String requestPath;

    public static AuditRequestInfo fraHttpServletRequest(HttpServletRequest request) {
        return AuditRequestInfo.builder()
                .callId(resolveCallId(request))
                .consumerId(resolveConsumerId(request))
                .requestMethod(request.getMethod())
                .requestPath(request.getRequestURI())
                .build();
    }

    public String getCallId() {
        return this.callId;
    }

    public String getConsumerId() {
        return this.consumerId;
    }

    public String getRequestMethod() {
        return this.requestMethod;
    }

    public String getRequestPath() {
        return this.requestPath;
    }
}
