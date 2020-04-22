package no.nav.common.abac;

import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.BaseAttribute;
import no.nav.common.abac.domain.request.Request;
import no.nav.common.abac.domain.request.XacmlRequest;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;


public class Utils {

    static boolean invalidClientValues(RequestData requestData) {
        return requestData.getDomain() == null
                || requestData.getCredentialResource() == null
                || (requestData.getOidcToken() == null && requestData.getSamlToken() == null && requestData.getSubjectId() == null)
                ;
    }

    public static String getResourceAttribute(XacmlRequest request, String requestedAttribute) {
        return Optional.ofNullable(request)
                .map(XacmlRequest::getRequest)
                .map(Request::getResource)
                .map(BaseAttribute::getAttribute)
                .map(findAttribute(requestedAttribute))
                .orElse("EMPTY");
    }

    private static Function<List<Attribute>, String> findAttribute(String requestedAttribute) {
        return attributes -> findAttribute(attributes, requestedAttribute);
    }

    private static String findAttribute(List<Attribute> attributes, String requestedAttribute) {
        return attributes.stream()
                .filter(a -> requestedAttribute.equals(a.getAttributeId()))
                .findFirst()
                .orElse(new Attribute("EMPTY", "EMPTY"))
                .getValue();
    }
}
