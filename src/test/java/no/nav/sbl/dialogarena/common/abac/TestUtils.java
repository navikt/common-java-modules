package no.nav.sbl.dialogarena.common.abac;

import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TestUtils {

    public static String getContentFromJsonFile(String filename) throws IOException {

        final URL url = Thread.currentThread().getContextClassLoader().getResource(filename);
        String path = getPathWithoutInitialSlashOnWindows(url);
        return Files.lines(Paths.get(path)).collect(Collectors.joining());
    }

    private static String getPathWithoutInitialSlashOnWindows(URL url) {
        return url.getPath().replaceFirst("^/(.:/)", "$1");
    }

    public static CloseableHttpResponse prepareResponse(int expectedResponseStatus, String expectedResponseBody) throws IOException {
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(expectedResponseBody.getBytes()));
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);

        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, expectedResponseStatus, ""));
        when(response.getEntity()).thenReturn(entity);
        return response;
    }
}
