package no.nav.sbl.dialogarena.common.abac;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;


class TestUtils {

    static String getContentFromJsonFile(String filename) throws IOException {

        final URL url = Thread.currentThread().getContextClassLoader().getResource(filename);
        String path = getPathWithoutInitialSlashOnWindows(url);
        return Files.lines(Paths.get(path)).collect(Collectors.joining());
    }

    private static String getPathWithoutInitialSlashOnWindows(URL url) {
        return url.getPath().replaceFirst("^/(.:/)", "$1");
    }
}
