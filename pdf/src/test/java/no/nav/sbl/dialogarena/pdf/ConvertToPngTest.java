package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsJpg;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.detect.IsPng;
import org.apache.commons.collections15.Transformer;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.ScaleMode.SCALE_TO_FIT_INSIDE_BOX;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.fitsInside;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.writeBytesToFile;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.junit.Assert.assertThat;

public class ConvertToPngTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertToPngTest.class);

    private static Dimension boundingBox;
    private Transformer<byte[], byte[]> transformer;

    @Before
    public void setup() {
        boundingBox = new Dimension(100, 150);
        transformer = new ConvertToPng(boundingBox, SCALE_TO_FIT_INSIDE_BOX);
    }

    @Test
    public void konvertererPdfTilPng() throws IOException {
        byte[] pdf = getBytesFromFile("/PdfToImageFiles/pdf-file.pdf");
        assertThat(pdf, match(new IsPdf()));
        long start = System.currentTimeMillis();
        byte[] png = transformer.transform(pdf);
        LOGGER.debug("{} tok {} ms", transformer.getClass(), System.currentTimeMillis() - start);
        assertThat(png, match(new IsPng()));

        ByteArrayInputStream bais = new ByteArrayInputStream(png);
        BufferedImage image = ImageIO.read(bais);

        assertThat(image, fitsInside(boundingBox));

        writeBytesToFile(png, "/PdfToImageFiles", "pdf-file-converted.png");
    }

    @Test
    public void konverterNAVBarnetrygdSkjemaTilPng() throws IOException {
        byte[] pdf = getBytesFromFile("/PdfToImageFiles/nav_barnetrygd.pdf");
        assertThat(pdf, match(new IsPdf()));
        long start = System.currentTimeMillis();
        byte[] png = transformer.transform(pdf);
        LOGGER.debug("{} tok {} ms", transformer.getClass(), System.currentTimeMillis() - start);
        assertThat(png, match(new IsPng()));

        ByteArrayInputStream bais = new ByteArrayInputStream(png);
        BufferedImage image = ImageIO.read(bais);

        assertThat(image, fitsInside(boundingBox));

        writeBytesToFile(png, "/PdfToImageFiles", "pdf-file-barnetrygd-converted.png");
    }

    @Test
    public void konvertererJpgTilPng() throws IOException {
        byte[] jpg = getBytesFromFile("/PdfToImageFiles/jpeg-file.jpeg");

        assertThat(jpg, match(new IsJpg()));
        byte[] png = transformer.transform(jpg);
        assertThat(png, match(new IsPng()));

        ByteArrayInputStream bais = new ByteArrayInputStream(png);
        BufferedImage image = ImageIO.read(bais);

        assertThat(image, fitsInside(boundingBox));

        writeBytesToFile(png, "/PdfToImageFiles", "jpeg-file-converted.jpeg");
    }

    @Test
    public void skalererPngUtenAaKonvertere() throws IOException {
        byte[] png = getBytesFromFile("/PdfToImageFiles/png-file.png");

        assertThat(png, match(new IsPng()));
        byte[] newPng = transformer.transform(png);
        assertThat(newPng, match(new IsPng()));

        ByteArrayInputStream bais = new ByteArrayInputStream(newPng);
        BufferedImage image = ImageIO.read(bais);

        assertThat(image, fitsInside(boundingBox));

        writeBytesToFile(newPng, "/PdfToImageFiles", "png-file-converted.png");
    }

    @Test(expected = IllegalArgumentException.class)
    public void kasterExceptionPaaUlovligFiltype() throws IOException {
        byte[] txt = getBytesFromFile("/PdfToImageFiles/illegal-file.txt");
        transformer.transform(txt);
    }
}
