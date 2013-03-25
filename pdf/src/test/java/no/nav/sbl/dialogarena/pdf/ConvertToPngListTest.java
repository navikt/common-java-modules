package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsJpg;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.detect.IsPng;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.awt.Dimension;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class ConvertToPngListTest {

    @Test
    public void convertPdfToPng() throws IOException {
        byte[] pdf = getBytesFromFile("/PdfToImageFiles/pdf-file.pdf");
        assertThat(pdf, match(new IsPdf()));

        List<byte[]> pngs = new ConvertToPngList(new Dimension(100, 150)).transform(pdf);
        assertThat(pngs, hasSize(6));

        for (byte [] png : pngs) {
           assertThat(png, match(new IsPng()));
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

    @Test
    public void convertJpgToPng() throws IOException {
        byte[] jpg = getBytesFromFile("/PdfToImageFiles/jpeg-file.jpeg");

        assertThat(jpg, match(new IsJpg()));
        List<byte[]> png = new ConvertToPngList(new Dimension(100, 150)).transform(jpg);
        assertThat(png.get(0), match(new IsPng()));
    }

    @Test
    public void dontChangePng() throws IOException {
        byte[] png = getBytesFromFile("/PdfToImageFiles/png-file.png");

        assertThat(png, match(new IsPng()));
        List<byte[]> newPng = new ConvertToPngList(new Dimension(100, 150)).transform(png);
        assertThat(newPng.get(0), match(new IsPng()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void kastExceptionPaaUlovligFiltype() throws IOException {
        byte[] txt = getBytesFromFile("/PdfToImageFiles/illegal-file.txt");
        List<byte[]> png = new ConvertToPngList(new Dimension(100, 150)).transform(txt);
    }
}
