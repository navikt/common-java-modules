package no.nav.common.abac.audit;

import no.nav.common.abac.constants.NavAttributter;
import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.BaseAttribute;
import no.nav.common.abac.domain.request.XacmlRequest;

import java.util.List;
import java.util.function.Predicate;

public class AuditLogFilterUtils {

    public static AuditLogFilter and(AuditLogFilter auditLogFilter1, AuditLogFilter auditLogFilter2) {
        return (auditRequestInfo, xacmlRequest, xacmlResponse) ->
                auditLogFilter1.isEnabled(auditRequestInfo, xacmlRequest, xacmlResponse) &&
                        auditLogFilter2.isEnabled(auditRequestInfo, xacmlRequest, xacmlResponse);
    }

    public static AuditLogFilter or(AuditLogFilter auditLogFilter1, AuditLogFilter auditLogFilter2) {
        return (auditRequestInfo, xacmlRequest, xacmlResponse) ->
                auditLogFilter1.isEnabled(auditRequestInfo, xacmlRequest, xacmlResponse) ||
                        auditLogFilter2.isEnabled(auditRequestInfo, xacmlRequest, xacmlResponse);
    }

    public static AuditLogFilter not(AuditLogFilter auditLogFilter) {
        return (auditRequestInfo, xacmlRequest, xacmlResponse) ->
                !auditLogFilter.isEnabled(auditRequestInfo, xacmlRequest, xacmlResponse);
    }

    /**
     * @param pathPredicate for filtrering på {@link AuditRequestInfo#getRequestPath}
     *
     * @return AuditLogFilter for predikat
     */
    public static AuditLogFilter pathFilter(Predicate<String> pathPredicate) {
        return (auditRequestInfo, xacmlRequest, xacmlResponse) ->
                pathPredicate.test(auditRequestInfo.getRequestPath());
    }
    /**
     * @param attributePredicate for filtrering på alle {@link Attribute} i {@link XacmlRequest}, se
     * {@link NavAttributter} for attributt-verdier
     *
     * @return AuditLogFilter for predikat
     */
    public static AuditLogFilter anyResourceAttributeFilter(Predicate<String> attributePredicate) {
        return (auditRequestInfo, xacmlRequest, xacmlResponse) ->
                xacmlRequest.getRequest().getResource().stream()
                        .map(BaseAttribute::getAttribute)
                        .flatMap(List::stream)
                        .map(Attribute::getValue)
                        .anyMatch(attributePredicate);
    }

}
