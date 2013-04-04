package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsJpg;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.detect.IsPng;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.writeBytesToFile;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ConvertToPngPdfBoxTest {
    private static final Logger logger = LoggerFactory.getLogger(ConvertToPngPdfBoxTest.class);

    @Test
    public void convertPdfToPng() throws IOException {
        byte[] pdf = getBytesFromFile("/PdfToImageFiles/pdf-file.pdf");
        Dimension frameDimension = new Dimension(100, 150);
        assertThat(pdf, match(new IsPdf()));
        long start = System.currentTimeMillis();
        byte[] png = new ConvertToPngPdfBox(frameDimension).transform(pdf);
        logger.debug("ConvertToPngPdfBox tok {} ms", System.currentTimeMillis() - start);
        assertThat(png, match(new IsPng()));

        ByteArrayInputStream bais = new ByteArrayInputStream(png);
        BufferedImage image = ImageIO.read(bais);
        assertThat((double) image.getWidth(), is(closeTo(frameDimension.getWidth(), 1.0)));
        assertThat((double) image.getHeight(), is(closeTo(frameDimension.getHeight(), 1.0)));

        writeBytesToFile(png, "/PdfToImageFiles", "pdf-file-converted.png");
    }

    @Test
    public void convertJpgToPng() throws IOException {
        byte[] jpg = getBytesFromFile("/PdfToImageFiles/jpeg-file.jpeg");
        Dimension frameDimension = new Dimension(150, 150);

        assertThat(jpg, match(new IsJpg()));
        byte[] png = new ConvertToPngPdfBox(frameDimension).transform(jpg);
        assertThat(png, match(new IsPng()));

        ByteArrayInputStream bais = new ByteArrayInputStream(png);
        BufferedImage image = ImageIO.read(bais);
        assertThat((double) image.getWidth(), is(closeTo(frameDimension.getWidth(), 1.0)));
        assertThat((double) image.getHeight(), is(closeTo(frameDimension.getHeight(), 1.0)));

        writeBytesToFile(png, "/PdfToImageFiles", "jpeg-file-converted.jpeg");
    }

    @Test
    public void dontChangePng() throws IOException {
        byte[] png = getBytesFromFile("/PdfToImageFiles/png-file.png");
        Dimension frameDimension = new Dimension(300, 100);

        assertThat(png, match(new IsPng()));
        byte[] newPng = new ConvertToPngPdfBox(frameDimension).transform(png);
        assertThat(newPng, match(new IsPng()));

        ByteArrayInputStream bais = new ByteArrayInputStream(newPng);
        BufferedImage image = ImageIO.read(bais);
        assertThat((double) image.getWidth(), is(closeTo(frameDimension.getWidth(), 1.0)));
        assertThat((double) image.getHeight(), is(closeTo(frameDimension.getHeight(), 1.0)));

        writeBytesToFile(newPng, "/PdfToImageFiles", "png-file-converted.png");
    }

    @Test(expected = IllegalArgumentException.class)
    public void kastExceptionPaaUlovligFiltype() throws IOException {
        byte[] txt = getBytesFromFile("/PdfToImageFiles/illegal-file.txt");
        Dimension frameDimension = new Dimension(100, 150);
        byte[] png = new ConvertToPngPdfBox(frameDimension).transform(txt);
    }
}
