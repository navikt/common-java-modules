package no.nav.common.abac;

import no.nav.common.abac.cef.CefEvent;
import no.nav.common.abac.cef.CefAbacEventContext;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Supplier;

import static no.nav.common.abac.cef.CefAbacEvent.createCefEvents;
import static org.slf4j.LoggerFactory.getLogger;

public class AuditLogger {

    private final Logger log;
    private final Supplier<Long> currentTimeInMillisSupplier;

    public AuditLogger() {
        this(getLogger("AuditLogger"), System::currentTimeMillis);
    }

    AuditLogger(Logger log, Supplier<Long> currentTimeInMillisSupplier) {
        this.log = log;
        this.currentTimeInMillisSupplier = currentTimeInMillisSupplier;
    }

    void logCef(XacmlRequest xacmlRequest, XacmlResponse xacmlResponse, CefAbacEventContext context) {
        List<CefEvent> cefEvents = createCefEvents(xacmlRequest, xacmlResponse, context, currentTimeInMillisSupplier);
        cefEvents.forEach(event -> log.info(event.toString()));
    }
}
