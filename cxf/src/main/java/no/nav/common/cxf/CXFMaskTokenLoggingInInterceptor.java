package no.nav.common.cxf;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.slf4j.Slf4jEventSender;

public class CXFMaskTokenLoggingInInterceptor implements LogEventSender {
    private boolean maskerToken = true;

    private final LogEventSender delegate = new Slf4jEventSender();

    public void setMaskerTokenLogging(boolean maskerTokenLogging) {
        this.maskerToken = maskerTokenLogging;
    }

    @Override
    public void send(LogEvent event) {
        if (maskerToken && event.getHeaders() != null) {
            event.getHeaders().remove("Cookie");
        }
        delegate.send(event);
    }
}
