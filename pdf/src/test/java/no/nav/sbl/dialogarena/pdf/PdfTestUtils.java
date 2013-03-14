package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;

public class PdfTestUtils {
    public static byte[] getBytesFromFile(String path) throws IOException {
        InputStream resourceAsStream = PdfTestUtils.class.getResourceAsStream(path);
        return IOUtils.toByteArray(resourceAsStream);
    }

    public static void  writeBytesToFile(byte[] bytes, String subDir, String filename) throws IOException {
        String directory = PdfTestUtils.class.getResource(subDir).getPath();
        File pdfFile = new File(directory + "/" + filename);

        FileOutputStream fos = new FileOutputStream(pdfFile);
        fos.write(bytes);
        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.isFile());
        fos.close();
    }
}
