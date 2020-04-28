package no.nav.common.abac;

import no.nav.common.abac.domain.request.Request;
import no.nav.common.abac.domain.response.Response;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.auth.SubjectHandler;
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
        log.info("Response from Abac - NAV-ident: " + SubjectHandler.getIdent().orElse("-") +
                " | Resource: " + request.getResource() +
                " | Decision value from ABAC: " + response.getDecision().name() +
                " | Pep-decision: " + biasedDecision +
                " | " + response.getAssociatedAdvice().toString());
    }
}
