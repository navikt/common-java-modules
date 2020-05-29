package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.common.auth.SubjectHandler;
import no.nav.log.cef.CefEvent;
import no.nav.sbl.dialogarena.common.abac.pep.cef.CefEventContext;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Request;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Response;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Supplier;

import static no.nav.sbl.dialogarena.common.abac.pep.cef.CefAbacEvent.createCefEvents;
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

    void logRequestInfo(Request request) {
        log.info("NAV-ident: " + SubjectHandler.getIdent().orElse("-") +
                " requests access to: " + request.getResource() +
                " with action : " + request.getAction());
    }

    void logResponseInfo(String biasedDecision, XacmlResponse xacmlResponse, Request request) {
        final Response response = xacmlResponse.getResponse().get(0);
        log.info("Respone from Abac - NAV-ident: " + SubjectHandler.getIdent().orElse("-") +
                " | Resource: " + request.getResource() +
                " | Decision value from ABAC: " + response.getDecision().name() +
                " | Pep-decision: " + biasedDecision +
                " | " + response.getAssociatedAdvice().toString());
    }


    void logCEF(XacmlResponse xacmlResponse, CefEventContext context) {
        List<CefEvent> cefEvents = createCefEvents(xacmlResponse, context, currentTimeInMillisSupplier);
        cefEvents.forEach(event -> log.info(event.toString()));
    }

}
