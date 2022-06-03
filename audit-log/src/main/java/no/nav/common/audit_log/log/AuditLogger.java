package no.nav.common.audit_log.log;

import no.nav.common.audit_log.cef.CefMessage;

public interface AuditLogger {

    /**
     * Write a CEF message to the logger.
     * This is the recommended way to audit log.
     * @param message the CEF formatted message that will be written to the audit log
     */
    void log(CefMessage message);

    /**
     * Write a raw message to the logger.
     * Can be used to write raw messages to the audit log, the user is responsible for the message format.
     * @param message the raw message that will be written to the audit log
     */
    void log(String message);

}
