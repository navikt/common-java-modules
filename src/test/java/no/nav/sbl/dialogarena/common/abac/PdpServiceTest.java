package no.nav.sbl.dialogarena.common.abac;

import mockit.Expectations;
import mockit.Tested;
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

import java.io.*;
import java.nio.file.Files;
import java.util.stream.Collectors;

import static java.nio.file.Paths.get;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

@RunWith(JMockit.class)
public class PdpServiceTest {

    @Tested
    PdpService pdpService;


    @Test
    public void convertsRequestToJson() throws IOException {
        PdpService pdpService = new PdpService();

        final StringEntity stringEntity = pdpService.convertRequestToJson(MockXacmlRequest.getXacmlRequest());

        assertThat(stringEntity.getContentLength(), greaterThan(0L));

        String expectedContent = getExpectedContentRequest();
        assertThat(getActualContent(stringEntity), is(expectedContent));
    }


    @Test
    public void returnsResponse() throws IOException {
        PdpService pdpService = new PdpService();

        new Expectations(PdpService.class) {{
            pdpService.doPost(withAny(new HttpPost()));
            result = prepareResponse(200, getExpectedContentResponse());
        }};

        final XacmlResponse xacmlResponse = pdpService.askForPermission(MockXacmlRequest.getXacmlRequest());
    }

    private static HttpResponse prepareResponse(int expectedResponseStatus, String expectedResponseBody){
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

    private static String getExpectedContentResponse() throws IOException {
        String expectedContent = Files.lines(get("C:\\code\\abac\\src\\test\\resources\\xacmlresponse.json")).collect(Collectors.joining());
        expectedContent = expectedContent.replaceAll("\\s", "");
        return expectedContent;
    }

    private String getExpectedContentRequest() throws IOException {
        String expectedContent = Files.lines(get("C:\\code\\abac\\src\\test\\resources\\xacmlrequest-withtoken.json")).collect(Collectors.joining());
        expectedContent = expectedContent.replaceAll("\\s", "");
        return expectedContent;
    }

    private String getActualContent(StringEntity stringEntity) throws IOException {
        final InputStream content = stringEntity.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(content));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();

    }
}

