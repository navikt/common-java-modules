package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Response;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditLogger {
    private static final Logger AUDITLOG = LoggerFactory.getLogger("auditlog");
    private static final Logger LOG = LoggerFactory.getLogger(AuditLogger.class);

    void logRequestInfo(String fnr) {
        String requestMessage = "NAV-ident: " + SubjectHandler.getSubjectHandler().getUid() + " requests access to fnr: " + fnr;

        AUDITLOG.info(requestMessage);
        LOG.info(requestMessage);
    }

    void logResponseInfo(String biasedDecision, XacmlResponse xacmlResponse, String fnr) {
        String decision = "";
        if (xacmlResponse.isFallbackUsed()) {
            decision = "FALLBACK ";
        }
        final Response response = xacmlResponse.getResponse().get(0);

        final String subjectidMessage = "NAV-ident: " + SubjectHandler.getSubjectHandler().getUid();
        final String fnrMessage = " | Fnr: " + fnr;
        final String decisionMessage = " | Decision value from ABAC: " + decision + response.getDecision().name();
        final String pepDecisionMessage = " | Pep-decision: " + biasedDecision;
        String responseMessage = subjectidMessage + fnrMessage + decisionMessage + " " + pepDecisionMessage;

        AUDITLOG.info(responseMessage);

        LOG.info(responseMessage + " | " + response.getAssociatedAdvice().toString());

    }

    public void log(String message) {
        AUDITLOG.info(message);
    }
}
