package no.nav.sbl.dialogarena.common.abac.pep;

import lombok.Builder;

@Builder
public class CEFEventContext {
    String callId;
    String consumerId;
    String requestMethod;
    String requestPath;
    String subjectId;
    CEFEventResource resource;
}
