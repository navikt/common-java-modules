package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsJpg;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.detect.IsPng;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ConvertToPngListTest {

    private static final Logger logger = LoggerFactory.getLogger(ConvertToPngListIcePdf.class);

    private static Dimension frameDimension = new Dimension(100, 150);

    private Transformer<byte[], List<byte[]>> icePdf = new ConvertToPngListIcePdf(frameDimension);
    private Transformer<byte[], List<byte[]>> pdfBox = new ConvertToPngListPdfBox(frameDimension);

    @Test
    public void konvertererPdfTilPngMedIcePdf() throws IOException {
        konvertererPdfTilPngListe(icePdf);
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
        konvertererPdfTilPngListe(pdfBox);
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

    private void konvertererPdfTilPngListe(Transformer<byte[], List<byte[]>> transformer) throws IOException {
        byte[] pdf = getBytesFromFile("/PdfToImageFiles/pdf-file.pdf");
        assertThat(pdf, match(new IsPdf()));

        long start = System.currentTimeMillis();
        List<byte[]> pngs = transformer.transform(pdf);
        logger.debug("{} tok {} ms", transformer.getClass(), System.currentTimeMillis() - start);

        assertThat(pngs, hasSize(6));

        for (byte [] png : pngs) {
            assertThat(png, match(new IsPng()));
            ByteArrayInputStream bais = new ByteArrayInputStream(png);
            BufferedImage image = ImageIO.read(bais);
            assertThat((double) image.getWidth(), is(closeTo(frameDimension.getWidth(), 1.0)));
            assertThat((double) image.getHeight(), is(closeTo(frameDimension.getHeight(), 1.0)));
        }

        try {
            String myDirectoryPath = ConvertToPngListTest.class.getResource("/PdfToImageFiles").getPath() + "/multiple-pdf-files-converted";
            File myDirectory = new File(myDirectoryPath);
            FileUtils.deleteDirectory(myDirectory);
            myDirectory.mkdir();
            for (int i = 0; i < pngs.size(); i++) {
                OutputStream out = new BufferedOutputStream(new FileOutputStream(myDirectoryPath + "/page" + Integer.toString(i + 1) + ".png"));
                out.write(pngs.get(i));
                out.close();
            }
        } catch  (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void konvertererJpgTilPng(Transformer<byte[], List<byte[]>> transformer) throws IOException {
        byte[] jpg = getBytesFromFile("/PdfToImageFiles/jpeg-file.jpeg");
        Dimension frameDimension = new Dimension(100, 150);

        assertThat(jpg, match(new IsJpg()));
        List<byte[]> png = transformer.transform(jpg);
        assertThat(png.get(0), match(new IsPng()));
        ByteArrayInputStream bais = new ByteArrayInputStream(png.get(0));
        BufferedImage image = ImageIO.read(bais);
        assertThat((double) image.getWidth(), is(closeTo(frameDimension.getWidth(), 1.0)));
        assertThat((double) image.getHeight(), is(closeTo(frameDimension.getHeight(), 1.0)));
    }

    private void skalererPngUtenAaKonvertere(Transformer<byte[], List<byte[]>> transformer) throws IOException {
        byte[] png = getBytesFromFile("/PdfToImageFiles/png-file.png");
        Dimension frameDimension = new Dimension(100, 150);
        assertThat(png, match(new IsPng()));
        List<byte[]> newPng = new ConvertToPngListIcePdf(frameDimension).transform(png);
        assertThat(newPng.get(0), match(new IsPng()));
        ByteArrayInputStream bais = new ByteArrayInputStream(newPng.get(0));
        BufferedImage image = ImageIO.read(bais);
        assertThat((double) image.getWidth(), is(closeTo(frameDimension.getWidth(), 1.0)));
        assertThat((double) image.getHeight(), is(closeTo(frameDimension.getHeight(), 1.0)));
    }

    private void kasterExceptionPaaUlovligFiltype(Transformer<byte[], List<byte[]>> transformer) throws IOException {
        byte[] txt = getBytesFromFile("/PdfToImageFiles/illegal-file.txt");
        Dimension frameDimension = new Dimension(100, 150);
        List<byte[]> png = new ConvertToPngListIcePdf(frameDimension).transform(txt);
    }
}
