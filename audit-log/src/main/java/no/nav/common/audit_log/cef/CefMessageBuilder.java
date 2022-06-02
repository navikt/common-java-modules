package no.nav.common.audit_log.cef;

import java.util.HashMap;
import java.util.Map;

import static no.nav.common.audit_log.cef.CefMessageExtensionFields.*;
import static no.nav.common.audit_log.log.AuditLoggerConstants.AUDIT_LOGGER_NAME;

public class CefMessageBuilder {
    private int version = 0;
    private String deviceVendor;
    private String deviceProduct = AUDIT_LOGGER_NAME;
    private String deviceVersion = "1.0";
    private String signatureId;
    private String name;
    private String severity;
    private final Map<String, String> extension = new HashMap<>();

    public CefMessageBuilder version(int version) {
        this.version = version;
        return this;
    }

    public CefMessageBuilder applicationName(String applicationName) {
        this.deviceVendor = applicationName;
        return this;
    }

    public CefMessageBuilder loggerName(String loggerName) {
        this.deviceProduct = loggerName;
        return this;
    }

    public CefMessageBuilder logFormatVersion(String logFormatVersion) {
        this.deviceVersion = logFormatVersion;
        return this;
    }

    public CefMessageBuilder event(CefMessageEvent event) {
        this.signatureId = event.name();
        return this;
    }

    public CefMessageBuilder event(String event) {
        this.signatureId = event;
        return this;
    }

    public CefMessageBuilder description(String description) {
        this.name = description;
        return this;
    }

    public CefMessageBuilder severity(CefMessageSeverity severity) {
        this.severity = severity.name();
        return this;
    }

    public CefMessageBuilder severity(String severity) {
        this.severity = severity;
        return this;
    }

    public CefMessageBuilder sourceUserId(String sourceUserId) {
        extension.put(FIELD_SOURCE_USER_ID, sourceUserId);
        return this;
    }

    public CefMessageBuilder destinationUserId(String destinationUserId) {
        extension.put(FIELD_DESTINATION_USER_ID, destinationUserId);
        return this;
    }

    public CefMessageBuilder timeEnded(long epochMillis) {
        extension.put(FIELD_END_TIME, String.valueOf(epochMillis));
        return this;
    }

    public CefMessageBuilder callId(String callId) {
        extension.put(FIELD_SPROC, callId);
        return this;
    }

    public CefMessageBuilder extension(String name, String value) {
        extension.put(name, value);
        return this;
    }

    public CefMessage build() {
        return new CefMessage(
                version,
                deviceVendor,
                deviceProduct,
                deviceVersion,
                signatureId,
                name,
                severity,
                extension
        );
    }

}
