package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class PdfTestUtils {
    public static byte[] getBytesFromFile(String path) throws IOException {
        InputStream resourceAsStream = PdfTestUtils.class.getResourceAsStream(path);
        return IOUtils.toByteArray(resourceAsStream);
    }

}
