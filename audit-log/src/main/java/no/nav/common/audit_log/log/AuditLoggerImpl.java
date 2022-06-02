package no.nav.common.audit_log.log;

import no.nav.common.audit_log.cef.CefMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.common.audit_log.log.AuditLoggerConstants.AUDIT_LOGGER_NAME;

public class AuditLoggerImpl implements AuditLogger {

    private final Logger log;

    public AuditLoggerImpl(Logger log) {
        this.log = log;
    }

    public AuditLoggerImpl() {
        log = LoggerFactory.getLogger(AUDIT_LOGGER_NAME);
    }

    @Override
    public void log(CefMessage message) {
        log.info(message.toString());
    }

    @Override
    public void log(String message) {
        log.info(message);
    }

}
