package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.fo.security.jwt.context.SubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AuditLogger {
    private static final Logger LOG = LoggerFactory.getLogger(AuditLogger.class);

    AuditLogger() {
    }

    void logRequestInfo(String fnr) {
        DateFormat df = new SimpleDateFormat("YYYY-MM-DD HH:MM:SS");
        Date date = new Date();
        log("Time of request: " + df.format(date));
        log("NAV-ident: " + SubjectHandler.getSubjectHandler().getUid());
        log("Fnr: " + fnr);
    }

    void logResponseInfo(String abacDecision, String biasedDecision, List<Advice> advises, boolean fallbackUsed) {
        String decision = "";
        if (fallbackUsed) {
            decision = "FALLBACK ";
        }
        log("Decision value from ABAC: " + decision + abacDecision);
        log("Pep decision: " + biasedDecision);
        if (advises != null) {
            log(advises.toString());
        }
    }

    void log(String message) {
        LOG.info(message);
    }

}
