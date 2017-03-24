package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.sbl.dialogarena.common.abac.pep.*;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.ClientErrorException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;

import static java.lang.System.getProperty;
import static no.nav.sbl.dialogarena.common.abac.pep.Utils.getApplicationProperty;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AbacService implements TilgangService {

    private static final String MEDIA_TYPE = "application/xacml+json";
    private static final Logger LOG = getLogger(AbacService.class);
    private final HttpLogger httpLogger = new HttpLogger();
    private final AuditLogger auditLogger = new AuditLogger();

    @Override
    public XacmlResponse askForPermission(XacmlRequest request) throws AbacException, IOException, NoSuchFieldException {
        HttpPost httpPost = getPostRequest(request);
        final HttpResponse rawResponse = doPost(httpPost);

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

    private boolean isSimulateConnectionProblem() {
        final String propertyConnectionProblem = getProperty("abac.bibliotek.simuler.avbrudd");
        return StringUtils.isNotBlank(propertyConnectionProblem) && propertyConnectionProblem.equals("true");
    }

    HttpResponse doPost(HttpPost httpPost) throws AbacException, NoSuchFieldException {
        final CloseableHttpClient httpClient = createHttpClient();

        HttpResponse response;
        try {
            if (isSimulateConnectionProblem()) {
                throw new SocketException("Simulating connection problem");
            }
            response = httpClient.execute(httpPost);
            auditLogger.log("HTTP response code: " + response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            httpLogger.logPostRequest(httpPost);
            httpLogger.logException("Error calling ABAC ", e);
            throw new AbacException("An error has occured calling ABAC: ", e);
        }
        return response;
    }

    private CloseableHttpClient createHttpClient() throws NoSuchFieldException {
        final RequestConfig config = createConfigForTimeout();

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .setDefaultCredentialsProvider(addSystemUserToRequest())
                .build();
    }

    private CredentialsProvider addSystemUserToRequest() throws NoSuchFieldException {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(
                getApplicationProperty(CredentialConstants.SYSTEMUSER_USERNAME),
                getApplicationProperty(CredentialConstants.SYSTEMUSER_PASSWORD));
        provider.setCredentials(AuthScope.ANY, credentials);
        return provider;
    }


    private RequestConfig createConfigForTimeout() {

        return RequestConfig.custom()
                .setConnectTimeout(getConnectionTimeout())
                .setConnectionRequestTimeout(getConnectionTimeout())
                .setSocketTimeout(getReadTimeout())
                .build();
    }

    private int getConnectionTimeout() {
        final String propertyConnectionTimeout = getProperty("abac.bibliotek.connectionTimeout");
        final int defaultConnectionTimeout = 500;
        return StringUtils.isNotBlank(propertyConnectionTimeout) ?
                Integer.parseInt(propertyConnectionTimeout) : defaultConnectionTimeout;
    }

    private int getReadTimeout() {
        final String propertyConnectionTimeout = getProperty("abac.bibliotek.readTimeout");
        final int defaultConnectionTimeout = 1500;
        return StringUtils.isNotBlank(propertyConnectionTimeout) ?
                Integer.parseInt(propertyConnectionTimeout) : defaultConnectionTimeout;
    }
}
