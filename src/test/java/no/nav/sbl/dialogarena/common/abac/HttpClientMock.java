package no.nav.sbl.dialogarena.common.abac;

import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;

@RunWith(JMockit.class)
public final class HttpClientMock extends MockUp<HttpClient>{


    @mockit.Mock
    HttpResponse execute(HttpUriRequest request) {
        return prepareResponse(200, "");
    }

    private HttpResponse prepareResponse(int expectedResponseStatus, String expectedResponseBody) {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), expectedResponseStatus, ""));
        response.setStatusCode(expectedResponseStatus);
        try {
            response.setEntity(new StringEntity(expectedResponseBody));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
        return response;
    }
}


