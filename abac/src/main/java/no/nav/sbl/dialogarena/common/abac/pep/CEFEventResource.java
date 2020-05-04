package no.nav.sbl.dialogarena.common.abac.pep;

import lombok.Value;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;

import java.util.Map;
import java.util.function.Function;

public abstract class CEFEventResource {

    private CEFEventResource() {}

    @Value
    public static class PersonIdResource extends CEFEventResource {
        AbacPersonId personId;
    }

    @Value
    public static class EnhetIdResource extends CEFEventResource {
        String enhet;
    }

    @Value
    public static class ListResource extends CEFEventResource {
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
