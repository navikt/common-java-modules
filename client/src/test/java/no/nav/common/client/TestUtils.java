package no.nav.common.client;

import lombok.SneakyThrows;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtils {

    @SneakyThrows
    public static String readTestResourceFile(String fileName) {
        URL fileUrl = TestUtils.class.getClassLoader().getResource(fileName);
        Path resPath = Paths.get(fileUrl.toURI());
        return Files.readString(resPath);
    }

    public static String readTestResourceFileWithoutWhitespace(String fileName) {
       return removeWhitespace(readTestResourceFile(fileName));
    }

    public static String removeWhitespace(String str) {
        return str
                .replaceAll(" ", "")
                .replaceAll("\t", "")
                .replaceAll("\n", "")
                .replaceAll("\r", "");
    }
}
