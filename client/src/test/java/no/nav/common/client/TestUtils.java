package no.nav.common.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtils {

    public static String readTestResourceFile(String fileName) throws IOException, URISyntaxException {
        URL fileUrl = TestUtils.class.getClassLoader().getResource(fileName);
        Path resPath = Paths.get(fileUrl.toURI());
        return Files.readString(resPath);
    }

    public static String readTestResourceFileWithoutWhitespace(String fileName) throws IOException, URISyntaxException {
        return removeWhitespace(readTestResourceFile(fileName));
    }

    public static String removeWhitespace(String str) {
        return str.replace(" ", "").replace("\t", "").replace("\n", "").replace("\r", "");
    }
}
