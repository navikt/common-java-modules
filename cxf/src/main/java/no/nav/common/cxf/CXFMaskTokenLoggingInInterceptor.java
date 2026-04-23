package no.nav.common.cxf;

import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.slf4j.Slf4jVerboseEventSender;

import java.util.LinkedHashMap;
import java.util.Map;

public class CXFMaskTokenLoggingInInterceptor extends LoggingInInterceptor {

    static final String COOKIE_HEADER = "Cookie";

    private boolean maskerToken = true;

    public CXFMaskTokenLoggingInInterceptor() {
        super();
        this.sender = new MaskingSender(new Slf4jVerboseEventSender());
    }

    public CXFMaskTokenLoggingInInterceptor(int limit) {
        super();
        setLimit(limit);
        this.sender = new MaskingSender(new Slf4jVerboseEventSender());
    }

    public void setMaskerTokenLogging(boolean maskerTokenLogging) {
        this.maskerToken = maskerTokenLogging;
    }

    /**
     * Fjerner Cookie-headeren fra LogEvent-et før logging, slik at tokens i
     * cookies ikke havner i loggene. Pakke-privat for å kunne testes direkte.
     */
    void maskIfEnabled(LogEvent event) {
        if (maskerToken && event.getHeaders() != null && event.getHeaders().containsKey(COOKIE_HEADER)) {
            Map<String, String> masked = new LinkedHashMap<>(event.getHeaders());
            masked.remove(COOKIE_HEADER);
            event.setHeaders(masked);
        }
    }

    private class MaskingSender implements LogEventSender {

        private final LogEventSender delegate;

        MaskingSender(LogEventSender delegate) {
            this.delegate = delegate;
        }

        @Override
        public void send(LogEvent event) {
            maskIfEnabled(event);
            delegate.send(event);
        }
    }
}
