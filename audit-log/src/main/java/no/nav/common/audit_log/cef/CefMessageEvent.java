package no.nav.common.audit_log.cef;

public enum CefMessageEvent {
    CREATE("audit:create"),
    ACCESS("audit:access"),
    UPDATE("audit:update"),
    DELETE("audit:delete");

    private final String event;

    CefMessageEvent(String event) {
        this.event = event;
    }
}