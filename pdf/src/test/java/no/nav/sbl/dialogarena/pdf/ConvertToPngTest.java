package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsJpg;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.detect.IsPng;
import org.apache.commons.collections15.Transformer;
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

public class ConvertToPngTest {

    private static final Logger logger = LoggerFactory.getLogger(ConvertToPngTest.class);
    private static Dimension frameDimension = new Dimension(100, 150);

    private Transformer<byte[], byte[]> icePdf = new ConvertToPngIcePdf(frameDimension);
    private Transformer<byte[], byte[]> pdfBox = new ConvertToPngPdfBox(frameDimension);

    @Test
    public void konvertererPdfTilPngMedIcePdf() throws IOException {
        konvertererPdfTilPng(icePdf);
    }
    
    @Test
    public void konvertererJpgTilPngMedIcePdf() throws IOException {
        konvertererJpgTilPng(icePdf);
    }

    @Test
    public void skalererPngUtenAaKonvertereMedIcePdf() throws IOException {
        skalererPngUtenAaKonvertere(icePdf);
    }

    @Test(expected = IllegalArgumentException.class)
    public void kasterExceptionPaaUlovligFiltypeMedIcePdf() throws IOException {
        kasterExceptionPaaUlovligFiltype(icePdf);
    }

    @Test
    public void konvertererPdfTilPngMedPdfBox() throws IOException {
        konvertererPdfTilPng(pdfBox);
    }

    @Test
    public void konvertererJpgTilPngMedPdfBox() throws IOException {
        konvertererJpgTilPng(pdfBox);
    }

    @Test
    public void skalererPngUtenAaKonvertereMedPdfBox() throws IOException {
        skalererPngUtenAaKonvertere(pdfBox);
    }

    @Test(expected = IllegalArgumentException.class)
    public void kasterExceptionPaaUlovligFiltypeMedPdfBox() throws IOException {
        kasterExceptionPaaUlovligFiltype(pdfBox);
    }
    

    private void konvertererPdfTilPng(Transformer<byte[], byte[]> transformer) throws IOException {
        byte[] pdf = getBytesFromFile("/PdfToImageFiles/pdf-file.pdf");
        assertThat(pdf, match(new IsPdf()));
        long start = System.currentTimeMillis();
        byte[] png = transformer.transform(pdf);
        logger.debug("{} tok {} ms", transformer.getClass(), System.currentTimeMillis() - start);
        assertThat(png, match(new IsPng()));

        ByteArrayInputStream bais = new ByteArrayInputStream(png);
        BufferedImage image = ImageIO.read(bais);
        assertThat((double) image.getWidth(), is(closeTo(frameDimension.getWidth(), 1.0)));
        assertThat((double) image.getHeight(), is(closeTo(frameDimension.getHeight(), 1.0)));

        writeBytesToFile(png, "/PdfToImageFiles", "pdf-file-converted.png");
    }

    private void konvertererJpgTilPng(Transformer<byte[], byte[]> transformer) throws IOException {
        byte[] jpg = getBytesFromFile("/PdfToImageFiles/jpeg-file.jpeg");

        assertThat(jpg, match(new IsJpg()));
        byte[] png = transformer.transform(jpg);
        assertThat(png, match(new IsPng()));

        ByteArrayInputStream bais = new ByteArrayInputStream(png);
        BufferedImage image = ImageIO.read(bais);
        assertThat((double) image.getWidth(), is(closeTo(frameDimension.getWidth(), 1.0)));
        assertThat((double) image.getHeight(), is(closeTo(frameDimension.getHeight(), 1.0)));

        writeBytesToFile(png, "/PdfToImageFiles", "jpeg-file-converted.jpeg");
    }

    private void skalererPngUtenAaKonvertere(Transformer<byte[], byte[]> transformer) throws IOException {
        byte[] png = getBytesFromFile("/PdfToImageFiles/png-file.png");

        assertThat(png, match(new IsPng()));
        byte[] newPng = transformer.transform(png);
        assertThat(newPng, match(new IsPng()));

        ByteArrayInputStream bais = new ByteArrayInputStream(newPng);
        BufferedImage image = ImageIO.read(bais);
        assertThat((double) image.getWidth(), is(closeTo(frameDimension.getWidth(), 1.0)));
        assertThat((double) image.getHeight(), is(closeTo(frameDimension.getHeight(), 1.0)));

        writeBytesToFile(newPng, "/PdfToImageFiles", "png-file-converted.png");
    }

    private void kasterExceptionPaaUlovligFiltype(Transformer<byte[], byte[]> transformer) throws IOException {
        byte[] txt = getBytesFromFile("/PdfToImageFiles/illegal-file.txt");
        byte[] png = transformer.transform(txt);
    }
}
