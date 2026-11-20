package no.nav.common.cxf.jetty;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import org.junit.Test;

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
