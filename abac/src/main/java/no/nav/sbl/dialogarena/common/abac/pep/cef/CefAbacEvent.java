package no.nav.sbl.dialogarena.common.abac.pep.cef;

import no.nav.log.cef.CefEvent;
import no.nav.sbl.dialogarena.common.abac.pep.NavAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;


public class CefAbacEvent {

    public static List<CefEvent> createCefEvents(XacmlResponse xacmlResponse,
                                                 CefEventContext context,
                                                 Supplier<Long> currentTimeInMillisSupplier) {

            CefEventResource resource = context.resource;

            return resource.getResourceToResponse().apply(xacmlResponse).stream().map(x -> {
                CefEvent.Builder cefEventBuilder = CefEvent.builder();

                addFromContext(cefEventBuilder, context, currentTimeInMillisSupplier);
                x.getAttributes().forEach(cefEventBuilder::addAttribute);
                addDecisionAttribute(x.getResponse(), cefEventBuilder);
                addDenyAttributes(x.getResponse(), cefEventBuilder);

                return cefEventBuilder.build();
            }).collect(toList());
    }

    private static void addFromContext(CefEvent.Builder cefEventBuilder,
                                       CefEventContext context,
                                       Supplier<Long> currentTimeInMillisSupplier) {
        cefEventBuilder
                .cefVersion("0")
                .applicationName(context.applicationName)
                .logName("Sporingslogg")
                .logFormatVersion("1.0")
                .eventType("audit:access")
                .description("ABAC Sporingslogg")
                .addAttribute("end", currentTimeInMillisSupplier.get().toString())
                .addAttribute("suid", context.subjectId)
                .addAttribute("sproc", context.callId)
                .addAttribute("dproc", context.consumerId)
                .addAttribute("requestMethod", context.requestMethod)
                .addAttribute("request", context.requestPath);
    }

    private static void addDecisionAttribute(Response response, CefEvent.Builder cefEventBuilder) {
        cefEventBuilder
                .severity(Decision.Permit.equals(response.getDecision()) ? CefEvent.Severity.INFO : CefEvent.Severity.WARN)
                .addAttribute("flexString1", response.getDecision().name())
                .addAttribute("flexString1Label", "Decision");
    }

    private static void addDenyAttributes(Response response, CefEvent.Builder cefEventBuilder) {
        if (!Decision.Permit.equals(response.getDecision())) {

            Advice advice = response.getAssociatedAdvice().get(0);

            getAttributeFromAdvice(NavAttributter.ADVICEOROBLIGATION_CAUSE, advice).ifPresent(cause -> {
                cefEventBuilder.addAttribute("cs3", cause.getValue());
                cefEventBuilder.addAttribute("cs3Label", "deny_cause");
            });

            getAttributeFromAdvice(NavAttributter.ADVICEOROBLIGATION_DENY_POLICY, advice).ifPresent(policy -> {
                cefEventBuilder.addAttribute("flexString2", policy.getValue());
                cefEventBuilder.addAttribute("flexString2Label", "deny_policy");
            });

            getAttributeFromAdvice(NavAttributter.ADVICEOROBLIGATION_DENY_RULE, advice).ifPresent(policy -> {
                cefEventBuilder.addAttribute("cs5", policy.getValue());
                cefEventBuilder.addAttribute("cs5Label", "deny_rule");
            });
        }
    }

    private static Optional<AttributeAssignment> getAttributeFromAdvice(String attribute, Advice advice) {
        return advice.getAttributeAssignment().stream().filter(attributeAssignment ->
                attribute.equals(attributeAssignment.getAttributeId()))
                .findFirst();
    }
}
