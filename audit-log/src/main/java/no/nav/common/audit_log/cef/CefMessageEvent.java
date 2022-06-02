package no.nav.common.audit_log.cef;

/**
 * Common event types for audit logging.
 * Is used in CefMessage.signatureId
 */
public enum CefMessageEvent {
    CREATE("audit:create"),
    ACCESS("audit:access"),
    UPDATE("audit:update"),
    DELETE("audit:delete");

    public final String type;

    CefMessageEvent(String type) {
        this.type = type;
    }
}