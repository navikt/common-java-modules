package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.sbl.dialogarena.common.abac.pep.XacmlMapper;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static no.nav.sbl.dialogarena.common.abac.pep.Utils.getApplicationProperty;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AbacService implements TilgangService {

    private static final String MEDIA_TYPE = "application/xacml+json";
    private static final Logger LOG = getLogger(AbacService.class);

    @Override
    public XacmlResponse askForPermission(XacmlRequest request) throws AbacException, IOException, NoSuchFieldException {
        HttpPost httpPost = getPostRequest(request);
        final HttpResponse rawResponse = doPost(httpPost);

        final int statusCode = rawResponse.getStatusLine().getStatusCode();
        if (statusCodeIn500Series(statusCode)) {
            throw new AbacException("An error has occured calling ABAC: " + rawResponse.getStatusLine().getReasonPhrase());
        }
        return XacmlMapper.mapRawResponse(rawResponse);
    }

    private boolean statusCodeIn500Series(int statusCode) {
        return statusCode >= 500 && statusCode < 600;
    }

    private HttpPost getPostRequest(XacmlRequest request) throws NoSuchFieldException, UnsupportedEncodingException {
        StringEntity postingString = XacmlMapper.mapRequestToEntity(request);
        final String abacEndpointUrl = getApplicationProperty("abac.endpoint.url");
        HttpPost httpPost = new HttpPost(abacEndpointUrl);
        httpPost.addHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE);
        httpPost.setEntity(postingString);
        return httpPost;
    }


    HttpResponse doPost(HttpPost httpPost) throws AbacException, NoSuchFieldException {
        final CloseableHttpClient httpClient = createHttpClient();

        HttpResponse response;
        try {
            response = httpClient.execute(httpPost);
            LOG.info("HTTP response code: " + response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            throw new AbacException("An error has occured calling ABAC: " + e.getMessage());
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
        final int connectionTimeout = 500;
        final int readTimeout = 1500;

        return RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setConnectionRequestTimeout(connectionTimeout)
                .setSocketTimeout(readTimeout)
                .build();
    }
}
