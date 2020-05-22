package no.nav.sbl.dialogarena.common.abac.pep.cef;

import lombok.EqualsAndHashCode;
import lombok.Value;
import no.nav.sbl.dialogarena.common.abac.pep.AbacPersonId;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;

import java.util.Map;
import java.util.function.Function;

@EqualsAndHashCode
public abstract class CefEventResource {

    private CefEventResource() {}

    @EqualsAndHashCode(callSuper = true)
    @Value
    public static class PersonIdResource extends CefEventResource {
        AbacPersonId personId;
    }

    @EqualsAndHashCode(callSuper = true)
    @Value
    public static class EnhetIdResource extends CefEventResource {
        String enhet;
    }

    @EqualsAndHashCode(callSuper = true)
    @Value
    public static class ListResource extends CefEventResource {
        Function<XacmlResponse, Map<String, Decision>> resourceToDecision;
    }

    public static PersonIdResource personId(AbacPersonId personId) {
        return new PersonIdResource(personId);
    }

    public static EnhetIdResource enhetId(String enhetId) {
        return new EnhetIdResource(enhetId);
    }

    public static ListResource list(Function<XacmlResponse, Map<String, Decision>> resourceToDecision) {
        return new ListResource(resourceToDecision);
    }
}
