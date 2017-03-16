package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AuditLogger {
    private static final Logger AUDITLOG = LoggerFactory.getLogger("auditlog");
    private static final Logger LOG = LoggerFactory.getLogger(AuditLogger.class);

    private final String linebreak = System.getProperty("line.separator");

    void logRequestInfo(String fnr) {
        String requestMessage = linebreak + "NAV-ident: " + SubjectHandler.getSubjectHandler().getUid() +
                linebreak + "Fnr: " + fnr;

        AUDITLOG.info(requestMessage);
        LOG.info(requestMessage);
    }

    void logResponseInfoWithAdvice(String biasedDecision, XacmlResponse xacmlResponse) {
        String decision = "";
        if (xacmlResponse.isFallbackUsed()) {
            decision = "FALLBACK ";
        }
        final Response response = xacmlResponse.getResponse().get(0);

        final String decisionMessage = "Decision value from ABAC: " + decision + response.getDecision().name();
        final String pepDecisionMessage = "Pep decision: " + biasedDecision;

        boolean logAdviceToSporbarhetslog = false;
        final List<Advice> associatedAdvice = response.getAssociatedAdvice();
        for (Advice advice : associatedAdvice) {
            if (advice.getId().equals("no.nav.abac.advices.action.sporbarhetslogg")) {
                logAdviceToSporbarhetslog = true;
            }
        }
        String responseMessage = linebreak +
                decisionMessage +
                linebreak +
                pepDecisionMessage;

        String responseMessageWithAdvices = responseMessage + linebreak + response.getAssociatedAdvice().toString();

        if (!logAdviceToSporbarhetslog) {
            AUDITLOG.info(responseMessage);
        } else {
            AUDITLOG.info(responseMessageWithAdvices);
        }
        LOG.info(responseMessageWithAdvices);

    }

    public void log(String message) {
        AUDITLOG.info(message);
    }
}
