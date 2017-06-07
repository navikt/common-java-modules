package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import no.nav.sbl.dialogarena.common.abac.pep.AuditLogger;
import no.nav.sbl.dialogarena.common.abac.pep.HttpLogger;
import no.nav.sbl.dialogarena.common.abac.pep.Utils;
import no.nav.sbl.dialogarena.common.abac.pep.XacmlMapper;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.ClientErrorException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static no.nav.abac.xacml.NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE;
import static no.nav.sbl.dialogarena.common.abac.pep.Utils.getApplicationProperty;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AbacService implements TilgangService {

    private static final String MEDIA_TYPE = "application/xacml+json";
    private static final Logger LOG = getLogger(AbacService.class);
    private final HttpLogger httpLogger = new HttpLogger();
    private final AuditLogger auditLogger = new AuditLogger();
    private final Abac abac;
    private final CloseableHttpClient httpClient;

    public AbacService(Abac abac, CloseableHttpClient httpClient) {
        this.abac = abac;
        this.httpClient = httpClient;
    }

    @Override
    public XacmlResponse askForPermission(XacmlRequest request) throws AbacException, IOException, NoSuchFieldException {
        CloseableHttpResponse rawResponse = null;
        try {
            HttpPost httpPost = getPostRequest(request);
            Timer timer = MetricsFactory.createTimer("abac-pdp");
            timer.start();
            rawResponse = doPost(httpPost);
            timer.stop();
            String ressursId = Utils.getResourceAttribute(request, RESOURCE_FELLES_RESOURCE_TYPE);
            timer.addTagToReport("resource-attributeid", ressursId);
            timer.report();

            final int statusCode = rawResponse.getStatusLine().getStatusCode();
            final String reasonPhrase = rawResponse.getStatusLine().getReasonPhrase();
            if (statusCodeIn500Series(statusCode)) {
                LOG.warn("ABAC returned: " + statusCode + " " + reasonPhrase);
                httpLogger.logPostRequest(httpPost);
                httpLogger.logHttpResponse(rawResponse);
                throw new AbacException("An error has occured calling ABAC: " + reasonPhrase);
            } else if (statusCodeIn400Series(statusCode)) {
                LOG.error("ABAC returned: " + statusCode + " " + reasonPhrase);
                httpLogger.logPostRequest(httpPost);
                httpLogger.logHttpResponse(rawResponse);
                throw new ClientErrorException("An error has occured calling ABAC: ", statusCode);
            }

            return XacmlMapper.mapRawResponse(rawResponse);
        } finally {
            if (rawResponse != null) {
                rawResponse.close();
            }
        }
    }

    private boolean statusCodeIn500Series(int statusCode) {
        return statusCode >= 500 && statusCode < 600;
    }

    private boolean statusCodeIn400Series(int statusCode) {
        return statusCode >= 400 && statusCode < 500;
    }

    private HttpPost getPostRequest(XacmlRequest request) throws NoSuchFieldException, UnsupportedEncodingException {
        StringEntity postingString = XacmlMapper.mapRequestToEntity(request);
        final String abacEndpointUrl = getApplicationProperty("abac.endpoint.url");
        HttpPost httpPost = new HttpPost(abacEndpointUrl);
        httpPost.addHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE);
        httpPost.setEntity(postingString);
        return httpPost;
    }

    private CloseableHttpResponse doPost(HttpPost httpPost) throws AbacException, NoSuchFieldException {

        CloseableHttpResponse response;
        try {
            response = abac.isAuthorized(this.httpClient, httpPost);
            auditLogger.log("HTTP response code from ABAC: " + response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            httpLogger.logPostRequest(httpPost);
            httpLogger.logException("Error calling ABAC ", e);
            throw new AbacException("An error has occured calling ABAC: ", e);
        }
        return response;
    }

}
