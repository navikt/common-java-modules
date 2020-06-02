package no.nav.sbl.dialogarena.common.abac.pep.cef;

import lombok.EqualsAndHashCode;
import lombok.Value;
import no.nav.sbl.dialogarena.common.abac.pep.AbacPersonId;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Response;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@EqualsAndHashCode
@Value
public class CefEventResource {

    Function<XacmlResponse, List<Context>> resourceToResponse;

    public CefEventResource(Function<XacmlResponse, List<Context>> resourceToResponse) {
        this.resourceToResponse = resourceToResponse;
    }

    @Value
    public static class Context {
        Response response;
        Map<String, String> attributes;
    }


    public static CefEventResource personId(AbacPersonId personId) {
        return new CefEventResource(response -> {
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("duid", personId.getId());
            Response resp = response.getResponse().get(0);
            return Collections.singletonList(new Context(resp, attributes));
        });
    }

    public static CefEventResource enhetId(String enhetId) {
        return new CefEventResource(response -> {
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("cs1", enhetId);
            Response resp = response.getResponse().get(0);
            return Collections.singletonList(new Context(resp, attributes));
        });
    }
}
