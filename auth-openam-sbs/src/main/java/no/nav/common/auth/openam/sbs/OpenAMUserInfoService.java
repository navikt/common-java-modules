package no.nav.common.auth.openam.sbs;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.sbl.rest.RestUtils;
import no.nav.sbl.util.StringUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static no.nav.sbl.rest.RestUtils.DEFAULT_CONFIG;
import static no.nav.sbl.util.StringUtils.of;

public class OpenAMUserInfoService {

    public static final String PARAMETER_UID = "uid";
    public static final String PARAMETER_SECURITY_LEVEL = "SecurityLevel";

    public static final String BASE_PATH = "/identity/json/attributes";

    private final URI endpointURL;
    private final Client client = RestUtils.createClient(DEFAULT_CONFIG.withDisableParameterLogging(true));
    private final List<String> subjectAttributes;
    private final OpenAMEventListener openAMEventListener;

    @Deprecated
    public OpenAMUserInfoService(URI endpointURL) {
        this(OpenAmConfig.builder().restUrl(endpointURL.toString()).build());
    }

    public OpenAMUserInfoService(OpenAmConfig openAmConfig) {
        this.openAMEventListener = openAmConfig.openAMEventListener;
        this.endpointURL = resolveEndpointURL(openAmConfig);
        this.subjectAttributes = subjectAttributes(openAmConfig);
    }

    private static List<String> subjectAttributes(OpenAmConfig openAmConfig) {
        List<String> attributes = new ArrayList<>();
        attributes.add(PARAMETER_UID);
        ofNullable(openAmConfig.additionalAttributes).ifPresent(attributes::addAll);
        return attributes;
    }

    private static URI resolveEndpointURL(OpenAmConfig openAmConfig) {
        String endpoint = openAmConfig.restUrl;
        URI uri = URI.create(endpoint);
        String scheme = uri.getScheme();
        if (StringUtils.nullOrEmpty(scheme)) {
            throw new IllegalStateException(endpoint);
        }
        return uri;
    }

    public Optional<Subject> convertTokenToSubject(String token) {
        return getUserInfo(token, subjectAttributes).flatMap(openAMAttributes -> createUserInfo(openAMAttributes, token));
    }

    public Optional<Map<String, String>> getUserInfo(String token, List<String> attributes) {
        return of(token).flatMap(t -> checkResponse(requestUserAttributes(t, attributes), token).map(this::attributesToMap));
    }

    public Response requestUserAttributes(String token) {
        return requestUserAttributes(token, subjectAttributes);
    }

    public Response requestUserAttributes(String token, List<String> attributes) {
        return client.target(getUrl(token, attributes)).request().get();
    }

    private Optional<OpenAMAttributes> checkResponse(Response response, String sessionId) {
        int status = response.getStatus();
        if (status < 399) {
            return of(response.readEntity(OpenAMAttributes.class));
        } else {
            String payload = response.readEntity(String.class);
            String phrase = response.getStatusInfo().getReasonPhrase();
            openAMEventListener.fetchingUserAttributesFailed(OpenAMEventListener.OpenAmResponse.builder()
                    .status(status)
                    .phrase(phrase)
                    .content(sanitize(payload, sessionId))
                    .build()
            );
            return empty();
        }
    }

    private String sanitize(String payload, String sessionId) {
        return payload.replaceAll(sessionId, "<session id removed>");
    }

    public String getUrl(String token) {
        return getUrl(token, subjectAttributes);
    }

    public String getUrl(String token, List<String> attributes) {
        UriBuilder uriBuilder = UriBuilder.fromUri(endpointURL).path(BASE_PATH).queryParam("subjectid", token);
        attributes.forEach(a -> uriBuilder.queryParam("attributenames", a));
        return uriBuilder.toString();
    }

    private Map<String, String> attributesToMap(OpenAMAttributes openAMAttributes) {
        return openAMAttributes.attributes.stream()
                .filter(a -> !a.values.isEmpty())
                .collect(toMap(
                        attribute -> attribute.name,
                        attribute -> attribute.values.get(0)
                ));
    }

    private Optional<Subject> createUserInfo(Map<String, String> attributeMap, String token) {
        if (attributeMap.containsKey(PARAMETER_UID)) {
            String uid = attributeMap.get(PARAMETER_UID);
            return of(new Subject(uid, IdentType.EksternBruker, SsoToken.eksternOpenAM(token, attributeMap)));
        } else {
            openAMEventListener.missingUserAttribute(PARAMETER_UID);
            return empty();
        }
    }

    @SuppressWarnings("unused")
    public static class OpenAMAttributes {
        private List<OpenAMAttribute> attributes = new ArrayList<>();
    }

    @SuppressWarnings("unused")
    public static class OpenAMAttribute {
        private String name;
        private List<String> values = new ArrayList<>();
    }

}
