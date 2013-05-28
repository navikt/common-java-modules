package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.writeBytesToFile;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class PdfWatermarkerTest {

    private static final String MOCK_FODSELSNUMMER = "112233 99999";
    private static final String INPUT_PDF = "skjema";

    private final DateTime time = new DateTime(2013, 3, 18, 14, 30, 30);

    private final PdfWatermarker watermarker = new PdfWatermarker();

    @Before
    public void initClock() {
        DateTimeUtils.setCurrentMillisFixed(time.getMillis());
    }

    @Test
    public void vannmerkerPdf() throws IOException {
        byte[] fileBytes = getBytesFromFile("/WatermarkerFiles/" + INPUT_PDF + ".pdf");
        byte[] watermarkedBytes = watermarker.forIdent(MOCK_FODSELSNUMMER).transform(fileBytes);

        assertThat(watermarkedBytes, match(new IsPdf()));
        assertThat(watermarkedBytes, not(fileBytes));

        String pdfText = extractText(watermarkedBytes);
        assertThat(pdfText, containsString(PdfWatermarker.LINE_1_HEADER));
        assertThat(pdfText, containsString(PdfWatermarker.LINE_2_HEADER));
        assertThat(pdfText, containsString(MOCK_FODSELSNUMMER));
        assertThat(pdfText, containsString("18.03.2013, kl. 14:30:30"));

        writeBytesToFile(watermarkedBytes, "/WatermarkerFiles", INPUT_PDF + "-vannmerket.pdf");
    }

    private String extractText(byte[] watermarkedBytes) {
        ByteArrayInputStream input = new ByteArrayInputStream(watermarkedBytes);
        PDDocument pdfDocument = null;
        try {
            pdfDocument = PDDocument.load(input);
            return new PDFTextStripper().getText(pdfDocument);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            try {
                if (pdfDocument != null) {
                    pdfDocument.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

    }

    @Test(expected = IllegalArgumentException.class)
    public void kasterExceptionForUlovligFil() throws IOException {
        byte[] png = getBytesFromFile("/ImageToPdfFiles/skjema1_side1.png");
        watermarker.forIdent("123123").transform(png);
    }
}
