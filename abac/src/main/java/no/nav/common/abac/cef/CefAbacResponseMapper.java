package no.nav.common.abac.cef;

import lombok.EqualsAndHashCode;
import lombok.Value;
import no.nav.common.abac.constants.NavAttributter;
import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.abac.domain.request.Resource;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.Response;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.EnhetId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

/**
 * Mapping av {@link XacmlRequest} og {@link XacmlResponse} til en liste som inneholder {@link Response} og et map for
 * ekstra attributter som skal logges.
 */
@EqualsAndHashCode
@Value
public class CefAbacResponseMapper {

    Function<Parameters, List<Result>> mapper;

    public CefAbacResponseMapper(Function<Parameters, List<Result>> mapper) {
        this.mapper = mapper;
    }

    @Value
    public static class Parameters {
        XacmlRequest request;
        XacmlResponse response;
    }

    @Value
    public static class Result {
        Response response;
        Map<String, String> customCefAttributes;
    }


    public static CefAbacResponseMapper personIdMapper(EksternBrukerId eksternBrukerId, ActionId actionId, Resource resource) {
        Map<String, String> attributes = commonAttributes(actionId, resource);
        attributes.put("duid", eksternBrukerId.get());
        return attributesMapper(attributes);
    }

    public static CefAbacResponseMapper enhetIdMapper(EnhetId enhetId, ActionId actionId, Resource resource) {
        Map<String, String> attributes = commonAttributes(actionId, resource);
        attributes.put("cs2", enhetId.get());
        return attributesMapper(attributes);
    }

    public static CefAbacResponseMapper resourceMapper(Resource resource) {
        Map<String, String> attributes = commonAttributes(null, resource);
        return attributesMapper(attributes);
    }

    public static CefAbacResponseMapper attributesMapper(Map<String, String> attributes) {
        return new CefAbacResponseMapper(parameters ->
                parameters.getResponse().getResponse().stream()
                        .map(res -> new Result(res, attributes)).collect(toList()));
    }

    private static Map<String, String> commonAttributes(ActionId actionId, Resource resource) {
        HashMap<String, String> attributes = new HashMap<>();
        Optional.ofNullable(actionId).map(action -> attributes.put("act", action.getId()));
        getAttribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, resource)
                .map(value -> attributes.put("requestContext", value));
        getAttribute(NavAttributter.RESOURCE_FELLES_DOMENE, resource)
                .map(value -> attributes.put("sourceServiceName", value));

        return attributes;
    }

    private static Optional<String> getAttribute(String attribute, Resource resource) {
        return resource.getAttribute().stream()
                .filter(x -> attribute.equals(x.getAttributeId()))
                .findFirst()
                .map(Attribute::getValue);
    }
}
