package no.nav.sbl.dialogarena.common.abac.pep.cef;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@ToString
@EqualsAndHashCode
public class CefEventContext {
    String applicationName;
    String callId;
    String requestMethod;
    String requestPath;
    String subjectId;
    CefEventResource resource;
}
