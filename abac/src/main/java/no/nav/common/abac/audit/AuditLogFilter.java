package no.nav.common.abac.audit;

@FunctionalInterface
public interface AuditLogFilter {
    boolean get(AuditRequestInfo auditRequestInfo);
}
