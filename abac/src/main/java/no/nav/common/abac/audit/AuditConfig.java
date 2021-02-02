package no.nav.common.abac.audit;

import lombok.Value;

@Value
public class AuditConfig {
    AuditLogger auditLogger;
    AuditRequestInfoSupplier auditRequestInfoSupplier;
    AuditLogFilter auditLogFilter;
}
