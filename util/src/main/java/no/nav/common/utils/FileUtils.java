package no.nav.common.utils;

import lombok.SneakyThrows;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FileUtils {

    @SneakyThrows
    public static String getResourceFileAsString(String resourceFilePath) {
        ClassLoader classLoader = FileUtils.class.getClassLoader();
        try (InputStream resourceStream = classLoader.getResourceAsStream(resourceFilePath)) {
            if (resourceStream == null) {
                throw new NullPointerException("Unable to get resource file " + resourceFilePath);
            }

            return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
