package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.common.auth.SubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Request;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Response;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class AuditLogger {

    private static final Logger log = getLogger("AuditLogger");

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
}
