package no.nav.sbl.dialogarena.pdf;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.junit.Test;

import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.writeBytesToFile;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class WatermarkerTest {

    private static final String MOCK_FODSELSNUMMER = "112233 12345";
    private static final String INPUT_PDF = "skjema";

    @Test
    public void vannmerkerPdf() throws IOException {
    	byte[] fileBytes = getBytesFromFile("/WatermarkerFiles/" + INPUT_PDF + ".pdf");

        Watermarker watermarker = new Watermarker(MOCK_FODSELSNUMMER);
        byte[] watermarkedBytes = watermarker.transform(fileBytes);
        assertThat(watermarkedBytes, match(new IsPdf()));
        assertThat(watermarkedBytes, not(fileBytes));

        // Sjekk at hver side i vannmerket PDF inneholder tekst fra vannmerket
        PdfReader reader = new PdfReader(watermarkedBytes);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        TextExtractionStrategy strategy;
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
            String pageText = strategy.getResultantText();
            assertThat(pageText, containsString(Watermarker.LINE_1_HEADER));
            assertThat(pageText, containsString(Watermarker.LINE_2_HEADER));
        }

        writeBytesToFile(watermarkedBytes, "/WatermarkerFiles", INPUT_PDF + "-vannmerket.pdf");
    }

    @Test(expected =  IllegalArgumentException.class)
    public void kasterExceptionForUlovligFil() throws IOException {
        byte[] png = getBytesFromFile("/ImageToPdfFiles/skjema1_side1.png");
        new Watermarker("123123").transform(png);
    }
}
