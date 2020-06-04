package no.nav.common.abac.cef;

import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Datastruktur for modellering av loggmeldinger i CEF-format
 */
public class CefEvent {

    public enum Severity {
        INFO,
        WARN
    }

    private final String cefVersion;
    private final String applicationName;
    private final String logName;
    private final String logFormatVersion;
    private final String eventType;
    private final String description;
    private final Severity severity;
    private final Map<String, String> attributes;

    private CefEvent(@NonNull String cefVersion,
                     @NonNull String applicationName,
                     @NonNull String logName,
                     @NonNull String logFormatVersion,
                     @NonNull String eventType,
                     @NonNull String description,
                     @NonNull Severity severity,
                     @NonNull Map<String, String> attributes) {
        this.cefVersion = cefVersion;
        this.applicationName = applicationName;
        this.logName = logName;
        this.logFormatVersion = logFormatVersion;
        this.eventType = eventType;
        this.description = description;
        this.severity = severity;
        this.attributes = attributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String cefVersion;
        private String applicationName;
        private String logName;
        private String logFormatVersion;
        private String eventType;
        private String description;
        private Severity severity;
        private final Map<String, String> attributes = new HashMap<>();

        public Builder() {
        }

        public Builder cefVersion(String cefVersion) {
            this.cefVersion = cefVersion;
            return this;
        }

        public Builder applicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        public Builder logName(String logName) {
            this.logName = logName;
            return this;
        }

        public Builder logFormatVersion(String logFormatVersion) {
            this.logFormatVersion = logFormatVersion;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder addAttribute(String key, String value) {
            if (value != null) {
                attributes.put(key, value);
            }
            return this;
        }

        public CefEvent build() {
            return new CefEvent(
                    cefVersion,
                    applicationName,
                    logName,
                    logFormatVersion,
                    eventType,
                    description,
                    severity,
                    attributes
            );
        }

    }

    @Override
    public String toString() {
        String extension = attributes.entrySet().stream()
                .map(entry -> format("%s=%s", entry.getKey(), escapeAttribute(entry.getValue())))
                .collect(Collectors.joining(" "));
        return
                format("CEF:%s|%s|%s|%s|%s|%s|%s|%s",
                        escapeHeader(cefVersion),
                        escapeHeader(applicationName),
                        escapeHeader(logName),
                        escapeHeader(logFormatVersion),
                        escapeHeader(eventType),
                        escapeHeader(description),
                        severity.name(),
                        extension);
    }

    private String escapeHeader(String header) {
        return header
                .replace("\\", "\\\\")
                .replace("|", "\\|");
    }

    private String escapeAttribute(String attribute) {
        return attribute
                .replace("\\", "\\\\")
                .replace("=", "\\=");
    }
}
