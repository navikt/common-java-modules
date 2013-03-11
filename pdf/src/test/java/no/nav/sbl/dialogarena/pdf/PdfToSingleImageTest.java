package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsJpg;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.detect.IsPng;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PdfToSingleImageTest {

    @Test
    public void convertsPdfToPng() throws IOException {
        byte[] pdf = getBytesFromFile("/PdfToImageFiles/pdf-file.pdf");
        assertThat(pdf, match(new IsPdf()));

        byte[] png = new PdfToSingleImage().transform(pdf);
        assertThat(png, match(new IsPng()));

        String directory = PdfToSingleImageTest.class.getResource("/PdfToImageFiles").getPath();
        File convertedPdf = new File(directory + "/" + "pdf-file-converted.png");

        FileOutputStream fos = new FileOutputStream(convertedPdf);
        fos.write(png);
        assertTrue(convertedPdf.exists());
        assertTrue(convertedPdf.isFile());
        fos.close();
    }

    @Test
    public void doesNothingWithJpg() throws IOException {
        byte[] jpg = getBytesFromFile("/PdfToImageFiles/jpeg-file.jpeg");

        assertThat(jpg, match(new IsJpg()));
        byte[] newJpg = new PdfToSingleImage().transform(jpg);
        assertThat(newJpg, match(new IsJpg()));
        assertThat(newJpg, is(jpg));
    }

    @Test
    public void doesNothingWithPng() throws IOException {
        byte[] png = getBytesFromFile("/PdfToImageFiles/png-file.png");

        assertThat(png, match(new IsPng()));
        byte[] newPng = new PdfToSingleImage().transform(png);
        assertThat(newPng, match(new IsPng()));
        assertThat(png, is(newPng));


    }
}
