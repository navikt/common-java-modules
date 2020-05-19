package no.nav.sbl.dialogarena.common.abac.pep.cef;

import lombok.Builder;

@Builder
public class CefEventContext {
    String applicationName;
    String callId;
    String requestMethod;
    String requestPath;
    String subjectId;
    CefEventResource resource;
}
