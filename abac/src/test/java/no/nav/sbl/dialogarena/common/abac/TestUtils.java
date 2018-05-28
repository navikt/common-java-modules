package no.nav.sbl.dialogarena.common.abac;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import no.nav.json.JsonUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;


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

    public static void assertJson(String json, String expectedJson) {
        ObjectNode objectNode = JsonUtils.fromJson(json, ObjectNode.class);
        ObjectNode expectedObjectNode = JsonUtils.fromJson(expectedJson, ObjectNode.class);
        assertThat(objectNode, equalTo(expectedObjectNode));
    }

}
