package no.nav.common.test.ssl;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SSLTestUtilsTest {

    @Test
    public void disableCertificateChecks() throws IOException {
        SSLTestUtils.disableCertificateChecks();
        File cxfFile = new File(System.getProperty("cxf.config.file"));
        assertTrue(cxfFile.isFile());
        assertEquals(cxfFile.getAbsolutePath(), System.getProperty("cxf.config.file"));
        File trustStore = new File(System.getProperty("javax.net.ssl.trustStore"));
        assertTrue(trustStore.isFile());
        assertNotEquals(cxfFile.getParentFile(), trustStore.getParentFile());
        assertPrivateFile(cxfFile.toPath());
        assertPrivateFile(trustStore.toPath());
    }

    private void assertPrivateFile(Path file) throws IOException {
        assertEquals(new File(System.getProperty("java.io.tmpdir")).getCanonicalFile(),
                file.getParent().getParent().toFile().getCanonicalFile());
        if (Files.getFileStore(file).supportsFileAttributeView("posix")) {
            assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(file.getParent()));
            assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file));
        }
    }

}