package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsJpg;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.detect.IsPng;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.writeBytesToFile;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.junit.Assert.assertThat;

public class ConvertToPngTest {
    private static final Logger logger = LoggerFactory.getLogger(ConvertToPngTest.class);

    @Test
    public void convertPdfToPng() throws IOException {
        byte[] pdf = getBytesFromFile("/PdfToImageFiles/pdf-file.pdf");
        assertThat(pdf, match(new IsPdf()));
        long start = System.currentTimeMillis();
        byte[] png = new ConvertToPng(new Dimension(100, 150)).transform(pdf);
        logger.debug("ConvertToPng tok {} ms", System.currentTimeMillis() - start);
        assertThat(png, match(new IsPng()));

        writeBytesToFile(png, "/PdfToImageFiles", "pdf-file-converted.png");
    }

    @Test
    public void convertJpgToPng() throws IOException {
        byte[] jpg = getBytesFromFile("/PdfToImageFiles/jpeg-file.jpeg");

        assertThat(jpg, match(new IsJpg()));
        byte[] png = new ConvertToPng(new Dimension(100, 150)).transform(jpg);
        assertThat(png, match(new IsPng()));
    }

    @Test
    public void dontChangePng() throws IOException {
        byte[] png = getBytesFromFile("/PdfToImageFiles/png-file.png");

        assertThat(png, match(new IsPng()));
        byte[] newPng = new ConvertToPng(new Dimension(100, 150)).transform(png);
        assertThat(newPng, match(new IsPng()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void kastExceptionPaaUlovligFiltype() throws IOException {
        byte[] txt = getBytesFromFile("/PdfToImageFiles/illegal-file.txt");
        byte[] png = new ConvertToPng(new Dimension(100, 150)).transform(txt);
    }
}
