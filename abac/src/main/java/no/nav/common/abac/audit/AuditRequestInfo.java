package no.nav.common.abac.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class AuditRequestInfo {
    private String callId;
    private String consumerId;
    private String requestMethod;
    private String requestPath;

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
