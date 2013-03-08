package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsPdf;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class WatermarkerTest {

    private static final String MOCK_FODSELSNUMMER = "112233 12345";
    private static final String INPUT_PDF = "skjema";

    @Test
    public void vannmerkerPdf() throws IOException {
    	byte[] fileBytes = getBytesFromFile("/WatermarkerFiles/" + INPUT_PDF + ".pdf");

        Watermarker watermarker = new Watermarker(MOCK_FODSELSNUMMER);
        byte[] watermarkedBytes = watermarker.transform(fileBytes);
        assertThat(watermarkedBytes, match(new IsPdf()));


        String directory = WatermarkerTest.class.getResource("/WatermarkerFiles").getPath();
        File stampedPdf = new File(directory + "/" + INPUT_PDF + "-vannmerket.pdf");

        FileOutputStream fos = new FileOutputStream(stampedPdf);
        fos.write(watermarkedBytes);
        assertTrue(stampedPdf.exists());
        assertTrue(stampedPdf.isFile());
        fos.close();
    }
}
