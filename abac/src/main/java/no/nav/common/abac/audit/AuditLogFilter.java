package no.nav.common.abac.audit;

import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;

@FunctionalInterface
public interface AuditLogFilter {
    boolean isEnabled(AuditRequestInfo auditRequestInfo, XacmlRequest xacmlRequest, XacmlResponse xacmlResponse);
}
