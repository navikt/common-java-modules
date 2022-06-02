package no.nav.common.audit_log.cef;

import lombok.Data;
import lombok.NonNull;

import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

/**
 * Represents a CEF (Common Event Format) message.
 */
@Data
public class CefMessage {
    private final int version;                      // Version
    private final String deviceVendor;              // Application name
    private final String deviceProduct;             // Name of the log that originated the event
    private final String deviceVersion;             // Version of the log format
    private final String signatureId;               // Event type
    private final String name;                      // Description
    private final String severity;                  // Severity of the event, usually "INFO" or "WARN"
    private final Map<String, String> extension;    // Additional attributes

    public CefMessage(
            int version,
            @NonNull String deviceVendor,
            @NonNull String deviceProduct,
            @NonNull String deviceVersion,
            @NonNull String signatureId,
            @NonNull String name,
            @NonNull String severity,
            @NonNull Map<String, String> extension
    ) {
        this.version = version;
        this.deviceVendor = deviceVendor;
        this.deviceProduct = deviceProduct;

        this.deviceVersion = deviceVersion;
        this.signatureId = signatureId;
        this.name = name;
        this.severity = severity;
        this.extension = extension;
    }

    public static CefMessageBuilder builder() {
        return new CefMessageBuilder();
    }

    /**
     * Returns a CEF formatted string representing this message.
     * Example: "CEF:Version|Device Vendor|Device Product|Device Version|Signature ID|Name|Severity|Extension"
     * @return CEF formatted string
     */
    @Override
    public String toString() {
        String extensionStr = extension.entrySet().stream()
                .map(entry -> format("%s=%s", entry.getKey(), escapeExtensionValue(entry.getValue())))
                .collect(joining(" "));

        return format(
                "CEF:%d|%s|%s|%s|%s|%s|%s|%s",
                    version,
                    escapeHeader(deviceVendor),
                    escapeHeader(deviceProduct),
                    escapeHeader(deviceVersion),
                    escapeHeader(signatureId),
                    escapeHeader(name),
                    escapeHeader(severity),
                    extensionStr
        );
    }

    private String escapeHeader(String header) {
        return header
                .replace("\\", "\\\\")
                .replace("|", "\\|");
    }

    private String escapeExtensionValue(String attribute) {
        return attribute
                .replace("\\", "\\\\")
                .replace("=", "\\=")
                .replace("\n", "\\n");
    }

}
