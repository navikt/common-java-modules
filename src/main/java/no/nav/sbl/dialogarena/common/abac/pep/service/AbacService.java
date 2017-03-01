package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.sbl.dialogarena.common.abac.pep.XacmlMapper;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AbacService implements TilgangService {

    private static final String MEDIA_TYPE = "application/xacml+json";
    private static final String pdpEndpointUrl = "https://e34wasl00401.devillo.no:9443/asm-pdp/authorize";
    private static final Logger LOG = getLogger(AbacService.class);

    @Override
    public XacmlResponse askForPermission(XacmlRequest request) throws AbacException {
        HttpPost httpPost = getPostRequest(request);
        final HttpResponse rawResponse = doPost(httpPost);

        if (rawResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new AbacException("An error has occured calling ABAC: " + rawResponse.getStatusLine().getReasonPhrase());
        }
        return XacmlMapper.mapRawResponse(rawResponse);
    }

    private HttpPost getPostRequest(XacmlRequest request) {
        StringEntity postingString = XacmlMapper.mapRequestToEntity(request);
        HttpPost httpPost = new HttpPost(pdpEndpointUrl);
        httpPost.addHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE);
        httpPost.setEntity(postingString);
        return httpPost;
    }

    HttpResponse doPost(HttpPost httpPost) throws AbacException {
        final RequestConfig config = createConfigForTimeout();
        final CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

        HttpResponse response;
        try {
            response = httpClient.execute(httpPost);
            LOG.info("HTTP response code: " + response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            throw new AbacException("An error has occured calling ABAC: " + e.getMessage());
        }
        return response;
    }


    private RequestConfig createConfigForTimeout() {
        final int timeoutInMillisec = 500;
        return RequestConfig.custom()
                .setConnectTimeout(timeoutInMillisec)
                .setConnectionRequestTimeout(timeoutInMillisec)
                .setSocketTimeout(timeoutInMillisec)
                .build();
    }
}
