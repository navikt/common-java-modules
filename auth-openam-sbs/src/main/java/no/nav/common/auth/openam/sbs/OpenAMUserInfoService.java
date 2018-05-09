package no.nav.common.auth.openam.sbs;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.sbl.rest.RestUtils;
import no.nav.sbl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toMap;
import static no.nav.sbl.rest.RestUtils.DEFAULT_CONFIG;

public class OpenAMUserInfoService {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAMUserInfoService.class);

    private static final String PARAMETER_UID = "uid";

    private static final String OPENAM_GENERAL_ERROR = "Could not get user attributes from OpenAM. ";
    public static final String BASE_PATH = "/identity/json/attributes";

    private final URI endpointURL;
    private final Client client = RestUtils.createClient(DEFAULT_CONFIG.withDisableParameterLogging(true));


    public OpenAMUserInfoService(OpenAmConfig openAmConfig) {
        endpointURL = resolveEndpointURL(openAmConfig);
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

    public OpenAMUserInfoService(URI endpointURL) {
        this.endpointURL = endpointURL;
    }

    public Optional<Subject> getUserInfo(String token) {
        return checkResponse(requestUserAttributes(token)).flatMap(openAMAttributes -> createUserInfo(openAMAttributes,token));
    }

    public Response requestUserAttributes(String token) {
        return client.target(getUrl(token)).request().get();
    }

    private Optional<OpenAMAttributes> checkResponse(Response response) {
        int status = response.getStatus();
        if (status < 399) {
            return of(response.readEntity(OpenAMAttributes.class));
        } else {
            String payload = response.readEntity(String.class);
            String phrase = response.getStatusInfo().getReasonPhrase();
            String message = OPENAM_GENERAL_ERROR + "HTTP status: " + status + " " + phrase + ".";
            if (status == 401) {
                message += " Response:" + payload;
            }
            LOG.error(message);
            return empty();
        }
    }

    public String getUrl(String token) {
        return endpointURL + BASE_PATH + format("?subjectid=%s&attributenames=%s", token, PARAMETER_UID);
    }

    private Optional<Subject> createUserInfo(OpenAMAttributes openAMAttributes, String token) {
        Map<String, String> attributeMap = openAMAttributes.attributes.stream().collect(toMap(
                attribute -> attribute.name,
                attribute -> attribute.values.get(0)
        ));

        if (attributeMap.containsKey(PARAMETER_UID)) {
            String uid = attributeMap.get(PARAMETER_UID);
            return of(new Subject(uid, IdentType.EksternBruker, SsoToken.eksternOpenAM(token)));
        } else {
            LOG.error(OPENAM_GENERAL_ERROR + "Response did not contain attribute " + PARAMETER_UID);
            return empty();
        }
    }

    @SuppressWarnings("unused")
    private static class OpenAMAttributes {
        private List<OpenAMAttribute> attributes = new ArrayList<>();
    }

    @SuppressWarnings("unused")
    private static class OpenAMAttribute {
        private String name;
        private List<String> values = new ArrayList<>();
    }

}
