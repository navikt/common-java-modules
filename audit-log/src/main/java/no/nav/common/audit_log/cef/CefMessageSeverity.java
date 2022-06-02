package no.nav.common.audit_log.cef;

/**
 * Common event types for audit logging.
 * Is used in CefMessage.severity
 */
public enum CefMessageSeverity {
    INFO, // Ex: An event that has occurred
    WARN  // Ex: An event where a person does not have access to a resource
}