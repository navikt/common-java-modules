package no.nav.sbl.dialogarena.common.abac.pep.cef;

import no.nav.log.cef.CefEvent;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Response;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CefAbacEvent {
    public static CefEvent createCefEvent(XacmlResponse xacmlResponse,
                                          CefEventContext context,
                                          Supplier<Long> currentTimeInMillisSupplier) {

        CefEvent.Builder cefEventBuilder = CefEvent.builder()
                .cefVersion("0")
                .applicationName(context.applicationName)
                .logName("Sporingslogg")
                .logFormatVersion("1.0")
                .eventType("trace:access")
                .description("ABAC Sporingslogg")
                .addAttribute("end", currentTimeInMillisSupplier.get().toString())
                .addAttribute("suid", context.subjectId)
                .addAttribute("sproc", context.callId)
                .addAttribute("requestMethod", context.requestMethod)
                .addAttribute("request", context.requestPath);


        if (context.resource instanceof CefEventResource.PersonIdResource) {
            CefEventResource.PersonIdResource resource = ((CefEventResource.PersonIdResource) context.resource);
            cefEventBuilder.addAttribute("duid", resource.getPersonId().getId());

            Response response = xacmlResponse.getResponse().get(0);
            Decision decision = response.getDecision();

            cefEventBuilder
                    .severity(Decision.Permit.equals(decision) ? CefEvent.Severity.INFO : CefEvent.Severity.WARN)
                    .addAttribute("flexString1", response.getDecision().name())
                    .addAttribute("flexString1Label", "Decision");

        } else if (context.resource instanceof CefEventResource.EnhetIdResource) {
            CefEventResource.EnhetIdResource resource = (CefEventResource.EnhetIdResource) context.resource;
            cefEventBuilder.addAttribute("deviceCustomString1", resource.getEnhet());

            Response response = xacmlResponse.getResponse().get(0);
            Decision decision = response.getDecision();

            cefEventBuilder
                    .severity(Decision.Permit.equals(decision) ? CefEvent.Severity.INFO : CefEvent.Severity.WARN)
                    .addAttribute("flexString1", decision.name())
                    .addAttribute("flexString1Label", "Decision");

        } else if (context.resource instanceof CefEventResource.ListResource) {

            CefEventResource.ListResource resource = (CefEventResource.ListResource) context.resource;

            Map<String, Decision> decisionMap = resource.getResourceToDecision().apply(xacmlResponse);

            List<String> resources = new ArrayList<>(decisionMap.keySet());
            List<Decision> decisions = new ArrayList<>(decisionMap.values());
            CefEvent.Severity severity = decisions.stream().anyMatch(decision ->
                    !Decision.Permit.equals(decision)) ? CefEvent.Severity.WARN : CefEvent.Severity.INFO;

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
