package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.BaseAttribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Request;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.System.getProperty;


public class Utils {

    public static String entityToString(HttpEntity stringEntity) throws IOException {
        final InputStream content;
        String result;
        try {
            content = stringEntity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(content));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            result = sb.toString();
        } catch (IOException e) {
            throw new IOException("Failed to parse json content to string", e);
        }
        return result;

    }

    static boolean invalidClientValues(Client client) {
        return client.getDomain() == null || client.getCredentialResource() == null ||
                (client.getOidcToken() == null && client.getSubjectId() == null);
    }

    public static String getApplicationProperty(String propertyKey) throws NoSuchFieldException {
        final String property = getProperty(propertyKey);
        if (StringUtils.isEmpty(property)) {
            throw new NoSuchFieldException("Cannot find property. Verify that property \"" + propertyKey + "\" is set.");
        }
        return property;
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
