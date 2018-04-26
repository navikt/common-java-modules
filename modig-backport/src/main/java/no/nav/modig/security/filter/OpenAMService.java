package no.nav.modig.security.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.sbl.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static java.lang.String.format;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;

/**
 * Service som sjekker om OpenAM token er gyldig eller ikke.
 */
public class OpenAMService {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAMService.class);

    public static final String OPENAM_REST_URL = "openam.restUrl";
    private static final String BASE_PATH = "/identity/json/isTokenValid";

    private final String restUrl;
    private final Client client = RestUtils.createClient();

    public OpenAMService() {
        restUrl = getOptionalProperty(OPENAM_REST_URL)
                .orElseThrow(() -> new RuntimeException("System property openam.restUrl er ikke tilgjengelig. Denne må være konfigurert opp for å kunne kjøre pålogging med OpenAM."));
    }

    public boolean isTokenValid(String tokenid) {
        LOG.debug("Sjekker om token er gyldig");
        String url = restUrl + BASE_PATH + format("?tokenid=%s", tokenid);
        Response response = client.target(url).request().get();
        int statusCode = response.getStatus();
        switch (statusCode) {
            case 200:
                return response.readEntity(IsTokenValidResponse.class).isValid;
            case 401:
                return false;
            default:
                throw new IllegalStateException(
                        "Kunne ikke verifisere om token er gyldig. Fikk statuskode '"
                                + statusCode + "'");
        }
    }

    private static class IsTokenValidResponse {
        @SuppressWarnings("unused")
        @JsonProperty("boolean")
        private boolean isValid;
    }

}
