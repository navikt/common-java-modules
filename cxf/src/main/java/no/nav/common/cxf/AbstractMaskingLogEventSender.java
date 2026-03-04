package no.nav.common.cxf;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.slf4j.Slf4jEventSender;

abstract class AbstractMaskingLogEventSender implements LogEventSender {

    private final LogEventSender delegate = new Slf4jEventSender();

    @Override
    public final void send(LogEvent event) {
        mask(event);
        delegate.send(event);
    }

    protected abstract void mask(LogEvent event);
}
