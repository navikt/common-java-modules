package no.nav.common.cxf;

import org.apache.cxf.ext.logging.event.LogEvent;

public class CXFMaskTokenLoggingInInterceptor extends AbstractMaskingLogEventSender {
    private boolean maskerToken = true;

    public void setMaskerTokenLogging(boolean maskerTokenLogging) {
        this.maskerToken = maskerTokenLogging;
    }

    @Override
    protected void mask(LogEvent event) {
        if (maskerToken && event.getHeaders() != null) {
            event.getHeaders().remove("Cookie");
        }
    }
}
