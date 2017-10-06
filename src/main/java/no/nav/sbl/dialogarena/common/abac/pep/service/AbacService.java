package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.sbl.dialogarena.common.abac.pep.Utils;
import no.nav.sbl.dialogarena.common.abac.pep.XacmlMapper;
import no.nav.sbl.dialogarena.common.abac.pep.context.CacheConfig;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import no.nav.sbl.rest.RestUtils;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static javax.ws.rs.client.Entity.entity;
import static no.nav.abac.xacml.NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE;
import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_PASSWORD;
import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_USERNAME;
import static no.nav.sbl.dialogarena.common.abac.pep.Utils.getApplicationProperty;
import static no.nav.sbl.dialogarena.common.abac.pep.Utils.timed;
import static org.glassfish.jersey.client.authentication.HttpAuthenticationFeature.basic;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AbacService implements TilgangService {

    public static final String ABAC_ENDPOINT_URL_PROPERTY_NAME = "abac.endpoint.url";

    private static final String MEDIA_TYPE = "application/xacml+json";
    private static final Logger LOG = getLogger(AbacService.class);
    private final Client client;


    public AbacService() {
        this(createClient());
    }

    AbacService(Client client) {
        this.client = client;
    }

    private static Client createClient() {
        Client client = RestUtils.createClient();
        client.register(basic(getApplicationProperty(SYSTEMUSER_USERNAME), getApplicationProperty(SYSTEMUSER_PASSWORD)));
        return client;
    }

    @Override
    @Cacheable(CacheConfig.ASK_FOR_PERMISSION)
    public XacmlResponse askForPermission(XacmlRequest request) throws AbacException, IOException, NoSuchFieldException {
        String ressursId = Utils.getResourceAttribute(request, RESOURCE_FELLES_RESOURCE_TYPE);
        Response response = timed(
                "abac-pdp",
                () -> request(request, client),
                (timer) -> timer.addTagToReport("resource-attributeid", ressursId)
        );

        final int statusCode = response.getStatus();
        final String reasonPhrase = response.getStatusInfo().getReasonPhrase();
        final String content = response.readEntity(String.class);

        if (statusCodeIn500Series(statusCode)) {
            LOG.warn("ABAC returned: " + statusCode + " " + reasonPhrase);
            throw new AbacException("An error has occured calling ABAC: " + reasonPhrase);
        } else if (statusCodeIn400Series(statusCode)) {
            LOG.error("ABAC returned: " + statusCode + " " + reasonPhrase);
            throw new ClientErrorException("An error has occured calling ABAC: ", statusCode);
        }
        return XacmlMapper.mapRawResponse(content);
    }

    private Response request(XacmlRequest request, Client client) {
        String postingString = XacmlMapper.mapRequestToEntity(request);
        final String abacEndpointUrl = getEndpointUrl();
        return client.target(abacEndpointUrl)
                .request()
                .post(entity(postingString, MEDIA_TYPE));
    }

    private boolean statusCodeIn500Series(int statusCode) {
        return statusCode >= 500 && statusCode < 600;
    }

    private boolean statusCodeIn400Series(int statusCode) {
        return statusCode >= 400 && statusCode < 500;
    }

    public static String getEndpointUrl() {
        return getApplicationProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME);
    }

}
