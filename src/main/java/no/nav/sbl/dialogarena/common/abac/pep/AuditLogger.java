package no.nav.sbl.dialogarena.common.abac.pep;

import lombok.extern.slf4j.Slf4j;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Request;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Response;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.utils.SecurityUtils;

@Slf4j
public class AuditLogger {

    void logRequestInfo(Request request) {
        log.info("NAV-ident: " + SecurityUtils.getIdent().orElse("-") +
                " requests access to: " + request.getResource() +
                " with action : " + request.getAction());
    }

    void logResponseInfo(String biasedDecision, XacmlResponse xacmlResponse, Request request) {
        final Response response = xacmlResponse.getResponse().get(0);
        log.info("Respone from Abac - NAV-ident: " + SecurityUtils.getIdent().orElse("-") +
                " | Resource: " + request.getResource() +
                " | Decision value from ABAC: " + response.getDecision().name() +
                " | Pep-decision: " + biasedDecision +
                " | " + response.getAssociatedAdvice().toString());
    }
}
