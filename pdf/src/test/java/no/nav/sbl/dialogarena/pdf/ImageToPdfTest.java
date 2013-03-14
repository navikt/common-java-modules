package no.nav.sbl.dialogarena.pdf;


import no.nav.sbl.dialogarena.detect.IsPdf;
import org.junit.Test;

import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.writeBytesToFile;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class ImageToPdfTest {

    private static final String IMAGE_DIR = "/ImageToPdfFiles";

    @Test
    public void konvertererIkkePdf() throws IOException {
        byte[] pdf = getBytesFromFile("/PdfMergerFiles/skjema1_side3.pdf");
        byte[] newPdf = new ImageToPdf().transform(pdf);
        assertThat(pdf, is(newPdf));
        assertThat(pdf, sameInstance(newPdf));
    }

    @Test
    public void konvertererPngTilPdf() throws Exception {
        byte[] imageBytes = getBytesFromFile(IMAGE_DIR + "/skjema1_side1.png");
        byte[] pdfBytes = new ImageToPdf().transform(imageBytes);

        assertThat(pdfBytes, match(new IsPdf()));
        assertThat(pdfBytes, is(not(imageBytes)));

        writeBytesToFile(pdfBytes, IMAGE_DIR, "skjema1_side1_fra_png.pdf");
    }

    @Test
    public void konvertererJpegTilPdf() throws Exception {
        byte[] imageBytes = getBytesFromFile(IMAGE_DIR + "/skjema1_side2.jpg");
        byte[] pdfBytes = new ImageToPdf().transform(imageBytes);

        assertThat(pdfBytes, match(new IsPdf()));
        assertThat(pdfBytes, is(not(imageBytes)));

        writeBytesToFile(pdfBytes, IMAGE_DIR, "skjema1_side2_fra_jpg.pdf");
    }
}
