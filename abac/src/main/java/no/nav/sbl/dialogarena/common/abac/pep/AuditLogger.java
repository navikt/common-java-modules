package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.common.auth.SubjectHandler;
import no.nav.log.cef.CEFEvent;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Request;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Response;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.sbl.util.EnvironmentUtils.requireApplicationName;
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


    void logCEF(XacmlResponse xacmlResponse, CEFEventContext context) {
        CEFEvent cefEvent = createCEFEvent(xacmlResponse, context);
        log.info(cefEvent.toString());
    }

    CEFEvent createCEFEvent(XacmlResponse xacmlResponse, CEFEventContext context) {

        CEFEvent.Builder cefEventBuilder = CEFEvent.builder()
                .cefVersion("0")
                .applicationName(requireApplicationName())
                .logName("Sporingslogg")
                .logFormatVersion("1.0")
                .eventType("audit:access")
                .description("ABAC Sporingslogg")
                .addAttribute("end", Long.toString(System.currentTimeMillis()))
                .addAttribute("suid", context.subjectId)
                .addAttribute("sproc", context.callId)
                .addAttribute("dproc", context.consumerId)
                .addAttribute("requestMethod", context.requestMethod)
                .addAttribute("request", context.requestPath);


        if (context.resource instanceof CEFEventResource.PersonIdResource) {
            CEFEventResource.PersonIdResource resource = ((CEFEventResource.PersonIdResource) context.resource);
            cefEventBuilder.addAttribute("duid", resource.getPersonId().getId());

            Response response = xacmlResponse.getResponse().get(0);
            Decision decision = response.getDecision();

            cefEventBuilder
                    .severity(Decision.Permit.equals(decision) ? CEFEvent.Severity.INFO : CEFEvent.Severity.WARN)
                    .addAttribute("flexString1", response.getDecision().name())
                    .addAttribute("flexString1Label", "Decision");

        } else if (context.resource instanceof CEFEventResource.EnhetIdResource) {
            CEFEventResource.EnhetIdResource resource = (CEFEventResource.EnhetIdResource) context.resource;
            cefEventBuilder.addAttribute("deviceCustomString2", resource.getEnhet());

            Response response = xacmlResponse.getResponse().get(0);
            Decision decision = response.getDecision();

            cefEventBuilder
                    .severity(Decision.Permit.equals(decision) ? CEFEvent.Severity.INFO : CEFEvent.Severity.WARN)
                    .addAttribute("flexString1", decision.name())
                    .addAttribute("flexString1Label", "Decision");

        } else if (context.resource instanceof CEFEventResource.ListResource) {

            CEFEventResource.ListResource resource = (CEFEventResource.ListResource) context.resource;

            Map<String, Decision> decisionMap = resource.getResourceToDecision().apply(xacmlResponse);

            List<String> resources = new ArrayList<>(decisionMap.keySet());
            List<Decision> decisions = new ArrayList<>(decisionMap.values());
            CEFEvent.Severity severity = decisions.stream().anyMatch(decision ->
                    !Decision.Permit.equals(decision)) ? CEFEvent.Severity.WARN : CEFEvent.Severity.INFO;

            cefEventBuilder
                    .severity(severity)
                    .addAttribute("cs3", String.join(" ", resources))
                    .addAttribute("cs3Label", "Resources")
                    .addAttribute("cs5", decisions.stream().map(Decision::name).collect(Collectors.joining(" ")))
                    .addAttribute("cs5Label", "Decisions");
        }

        return cefEventBuilder.build();
    }
}
