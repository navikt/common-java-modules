package no.nav.common.aktorregisterklient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtils {

    public static String readTestResourceFile(String fileName) {
        try {
            URL fileUrl = TestUtils.class.getClassLoader().getResource(fileName);
            Path resPath = Paths.get(fileUrl.toURI());
            return new String(Files.readAllBytes(resPath), StandardCharsets.UTF_8);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
