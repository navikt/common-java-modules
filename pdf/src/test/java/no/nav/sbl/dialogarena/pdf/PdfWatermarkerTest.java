package no.nav.sbl.dialogarena.pdf;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.time.FreezedClock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.writeBytesToFile;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class PdfWatermarkerTest {

    private static final String MOCK_FODSELSNUMMER = "112233 12345";
    private static final String INPUT_PDF = "skjema";

    private final DateTime time = new DateTime(2013, 3, 18, 14, 30, 30);

    private FreezedClock clock = new FreezedClock();
    private final PdfWatermarker watermarker = new PdfWatermarker(clock);

    @Before
    public void initClock() {
        clock.set(time);
    }

    @Test
    public void vannmerkerPdf() throws IOException {
    	byte[] fileBytes = getBytesFromFile("/WatermarkerFiles/" + INPUT_PDF + ".pdf");

        byte[] watermarkedBytes = watermarker.forIdent(MOCK_FODSELSNUMMER).transform(fileBytes);
        assertThat(watermarkedBytes, match(new IsPdf()));
        assertThat(watermarkedBytes, not(fileBytes));

        // Sjekk at hver side i vannmerket PDF inneholder tekst fra vannmerket
        PdfReader reader = new PdfReader(watermarkedBytes);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        TextExtractionStrategy strategy;
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
            String pageText = strategy.getResultantText();
            assertThat(pageText, containsString(PdfWatermarker.LINE_1_HEADER));
            assertThat(pageText, containsString(PdfWatermarker.LINE_2_HEADER));
            assertThat(pageText, containsString(MOCK_FODSELSNUMMER));
            assertThat(pageText, containsString("18.03.2013, kl. 14:30:30"));
        }

        writeBytesToFile(watermarkedBytes, "/WatermarkerFiles", INPUT_PDF + "-vannmerket.pdf");
    }

    @Test(expected =  IllegalArgumentException.class)
    public void kasterExceptionForUlovligFil() throws IOException {
        byte[] png = getBytesFromFile("/ImageToPdfFiles/skjema1_side1.png");
        watermarker.forIdent("123123").transform(png);
    }
}
