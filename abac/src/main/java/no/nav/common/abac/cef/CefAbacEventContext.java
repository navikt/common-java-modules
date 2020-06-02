package no.nav.common.abac.cef;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@ToString
@EqualsAndHashCode
public class CefAbacEventContext {
    String applicationName;
    String callId;
    String consumerId;
    String requestMethod;
    String requestPath;
    String subjectId;
    CefAbacResponseMapper mapper;

}
