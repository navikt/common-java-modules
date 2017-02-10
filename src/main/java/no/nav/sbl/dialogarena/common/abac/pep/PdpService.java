package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdpService {

    private final static String MEDIA_TYPE = "application/xacml+json";
    private static final String pdpEndpointUrl = "https://e34wasl00401.devillo.no:9443/asm-pdp/authorize";


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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }


}
