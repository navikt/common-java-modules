package no.nav.common.abac;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


public class TestUtils {

    @SneakyThrows
    public static String getContentFromJsonFile(String filename) {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(filename);
        String path = getPathWithoutInitialSlashOnWindows(url);
        return Files.lines(Paths.get(path)).collect(Collectors.joining());
    }

    private static String getPathWithoutInitialSlashOnWindows(URL url) {
        return url.getPath().replaceFirst("^/(.:/)", "$1");
    }

    public static void assertJsonEquals(String expectedJson, String actualJson) {
        JsonElement expectedElement = JsonParser.parseString(expectedJson);
        JsonElement actualElement = JsonParser.parseString(actualJson);
        assertEquals(expectedElement, actualElement);
    }

}
