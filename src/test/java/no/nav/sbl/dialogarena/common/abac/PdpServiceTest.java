package no.nav.sbl.dialogarena.common.abac;

import mockit.Expectations;
import mockit.integration.junit4.JMockit;
import no.nav.sbl.dialogarena.common.abac.pep.MockXacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.XacmlResponse;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@RunWith(JMockit.class)
public class PdpServiceTest {

    @Test
    public void returnsResponse() throws IOException {
        PdpService pdpService = new PdpService();

        new Expectations(PdpService.class) {{
            pdpService.doPost(withAny(new HttpPost()));
            result = prepareResponse(200, getExpectedContentResponse());
        }};

        final XacmlResponse actualXacmlResponse = pdpService.askForPermission(MockXacmlRequest.getXacmlRequest());

        //TODO Assert....

    }

    private static HttpResponse prepareResponse(int expectedResponseStatus, String expectedResponseBody) throws UnsupportedEncodingException {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), expectedResponseStatus, ""));
        response.setStatusCode(expectedResponseStatus);
        response.setEntity(new StringEntity(expectedResponseBody));
        return response;
    }

    private static String getExpectedContentResponse() throws IOException {

        final URL url = Thread.currentThread().getContextClassLoader().getResource("xacmlresponse.json");
        String path = getPathWithoutInitialSlashOnWindows(url);
        return Files.lines(Paths.get(path)).collect(Collectors.joining());
    }

    private static String getPathWithoutInitialSlashOnWindows(URL url) {
        return url.getPath().replaceFirst("^/(.:/)", "$1");
    }


}

