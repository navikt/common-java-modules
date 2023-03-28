package no.nav.common.audit_log.cef;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
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
        this.signatureId = event.type;
        return this;
    }

    public CefMessageBuilder event(String event) {
        this.signatureId = event;
        return this;
    }

    public CefMessageBuilder name(String name) {
        this.name = name;
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

    public CefMessageBuilder extensions(Map<String, String> extensions) {
        extension.putAll(extensions);
        return this;
    }

    /**
     * Creates a flexString{position}/flexString{position}Label extension pair. Used to add custom data to the message.
     * The flexStringLabel should contain a string that describes the value inside flexString.
     * Ex: {@code flexString(1, "Decision", "Permit") -> flexString1Label=Decision flexString1=Permit}
     * @param position added onto flexString and flexStringLabel to allow for multiple pairs, must be either 1 or 2
     * @param label the value for flexStringLabel, should describe the value in flexString
     * @param value the value for flexString
     * @return the builder
     */
    public CefMessageBuilder flexString(int position, String label, String value) {
        if (position < 1 || position > 2)
            throw new IllegalArgumentException("position must be either 1 or 2");

        extension.put(format("flexString%dLabel", position), label);
        extension.put("flexString" + position, value);

        return this;
    }

    /**
     * Creates a cs{position}/cs{position}Label extension pair. Used to add custom data to the message.
     * The csLabel should contain a string that describes the value inside cs.
     * Ex: {@code customString(1, "Decision", "Permit") -> cs1Label=Decision cs1=Permit}
     * @param position added onto cs and csLabel to allow for multiple pairs, must be between 1 to 6 inclusive
     * @param label the value for csLabel, should describe the value in cs
     * @param value the value for cs
     * @return the builder
     */
    public CefMessageBuilder customString(int position, String label, String value) {
        if (position < 1 || position > 6)
            throw new IllegalArgumentException("position must be a value from 1 to 6 inclusive");

        extension.put(format("cs%dLabel", position), label);
        extension.put("cs" + position, value);

        return this;
    }

    /**
     * Hvis du oppretter en CEF-melding i forbindelse med en tilgangskontroll sjekk så bør det logges resultatet av sjekken.
     * Denne hjelpefunksjonen gjør det enklere å sette opp en CEF-melding med riktig felter.
     * @param decision resultatet av tilgangskontroll sjekken
     * @return the builder
     */
    public CefMessageBuilder authorizationDecision(AuthorizationDecision decision) {
        return this.severity(authDecisionToSeverity(decision)).flexString(1, "Decision",  authDecisionToStr(decision));
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

    private String authDecisionToStr(AuthorizationDecision decision) {
        switch (decision) {
            case DENY:
                return "Deny";
            case PERMIT:
                return "Permit";
            default:
                throw new IllegalArgumentException("Unable to map to string for unknown decision " + decision.name());
        }
    }

    private CefMessageSeverity authDecisionToSeverity(AuthorizationDecision decision) {
        switch (decision) {
            case DENY:
                return CefMessageSeverity.WARN;
            case PERMIT:
                return CefMessageSeverity.INFO;
            default:
                throw new IllegalArgumentException("Unable to find severity for unknown decision " + decision.name());
        }
    }

}
