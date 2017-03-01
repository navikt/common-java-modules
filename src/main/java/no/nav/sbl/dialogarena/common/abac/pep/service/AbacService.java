package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.sbl.dialogarena.common.abac.pep.XacmlMapper;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AbacService implements TilgangService {

    private final static String MEDIA_TYPE = "application/xacml+json";
    private static final String pdpEndpointUrl = "https://e34wasl00401.devillo.no:9443/asm-pdp/authorize";

    @Override
    public XacmlResponse askForPermission(XacmlRequest request) throws AbacException {
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
        } catch (IOException e) {
            throw new AbacException("An error has occured calling ABAC: " + e.getMessage());
        }
        return response;
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
