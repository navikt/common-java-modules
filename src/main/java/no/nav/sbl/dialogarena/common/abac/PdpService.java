package no.nav.sbl.dialogarena.common.abac;

import com.google.gson.Gson;
import no.nav.sbl.dialogarena.common.abac.pep.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.XacmlResponse;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Component
public class PdpService {

    private final static String MEDIA_TYPE = "application/xacml+json";
    private static final String pdpEndpointUrl = "https://e34wasl00401.devillo.no:9443/asm-pdp/authorize";


    public XacmlResponse askForPermission(XacmlRequest request) {
        HttpPost httpPost = getPostRequest(request);
        System.out.println("Ask for permission");
        final HttpResponse response = doPost(httpPost);


        return null;
    }

    private HttpPost getPostRequest(XacmlRequest request) {
        StringEntity postingString = convertRequestToJson(request);
        HttpPost httpPost = new HttpPost(pdpEndpointUrl);
        httpPost.addHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE);
        httpPost.setEntity(postingString);
        return httpPost;
    }

     HttpResponse doPost(HttpPost httpPost) {
        final CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        HttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    StringEntity convertRequestToJson(XacmlRequest request) {
        Gson gson = new Gson();
        StringEntity postingString = null;
        try {
            postingString = new StringEntity(gson.toJson(request));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return postingString;
    }
}
