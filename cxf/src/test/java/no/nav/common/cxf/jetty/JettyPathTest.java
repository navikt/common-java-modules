package no.nav.common.cxf.jetty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class JettyPathTest {

    @Test
    public void findsModuleSourcesFromGradleTestClasses() throws IOException {
        assertEquals(
                new File("src/main/webapp").getCanonicalFile(),
                Jetty.getFileSource("src/main/webapp").getCanonicalFile());
        assertEquals(
                new File("src/test/resources").getCanonicalFile(),
                Jetty.getFileSource("src/test/resources").getCanonicalFile());
    }
}
