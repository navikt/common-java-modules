package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class PdpService {

    private static final String MEDIA_TYPE = "application/xacml+json";
    private static final String pdpEndpointUrl = "https://e34wasl00401.devillo.no:9443/asm-pdp/authorize";
    private static final Logger log = getLogger(PdpService.class);

    public XacmlResponse askForPermission(XacmlRequest request) {
        HttpPost httpPost = getPostRequest(request);
        final HttpResponse rawResponse = doPost(httpPost);
        return XacmlMapper.mapRawResponse(rawResponse);
    }

    private HttpPost getPostRequest(XacmlRequest request) {
        StringEntity postingString = XacmlMapper.mapRequestToEntity(request);
        HttpPost httpPost = new HttpPost(pdpEndpointUrl);
        httpPost.addHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE);
        httpPost.setEntity(postingString);
        return httpPost;
    }

    public HttpResponse doPost(HttpPost httpPost) {
        final CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        HttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
            log.info("HTTP response code: " + response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
}
