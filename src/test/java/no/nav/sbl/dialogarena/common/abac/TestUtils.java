package no.nav.sbl.dialogarena.common.abac;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;


public class TestUtils {

    public static String getContentFromJsonFile(String filename) throws IOException {

        final URL url = Thread.currentThread().getContextClassLoader().getResource(filename);
        String path = getPathWithoutInitialSlashOnWindows(url);
        return Files.lines(Paths.get(path)).collect(Collectors.joining());
    }

    private static String getPathWithoutInitialSlashOnWindows(URL url) {
        return url.getPath().replaceFirst("^/(.:/)", "$1");
    }

    public static HttpResponse prepareResponse(int expectedResponseStatus, String expectedResponseBody) throws UnsupportedEncodingException {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), expectedResponseStatus, ""));
        response.setStatusCode(expectedResponseStatus);
        response.setEntity(new StringEntity(expectedResponseBody));
        return response;
    }
}
