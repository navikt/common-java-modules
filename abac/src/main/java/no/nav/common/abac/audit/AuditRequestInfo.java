package no.nav.common.abac.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;

import java.util.function.BooleanSupplier;

import static no.nav.common.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.common.log.LogFilter.resolveCallId;

@Builder
@Getter
@AllArgsConstructor
public class AuditRequestInfo {
    private String callId;
    private String consumerId;
    private String requestMethod;
    private String requestPath;
    private BooleanSupplier filter;

    public static AuditRequestInfo fraHttpServletRequest(HttpServletRequest request) {
        return fraHttpServletRequest(request, () -> true);
    }

    public static AuditRequestInfo fraHttpServletRequest(HttpServletRequest request, BooleanSupplier filter) {
        return AuditRequestInfo.builder()
                .callId(resolveCallId(request))
                .consumerId(request.getHeader(CONSUMER_ID_HEADER_NAME))
                .requestMethod(request.getMethod())
                .requestPath(request.getRequestURI())
                .filter(filter)
                .build();
    }
}
