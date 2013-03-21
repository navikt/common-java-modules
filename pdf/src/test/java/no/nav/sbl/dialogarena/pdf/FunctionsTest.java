package no.nav.sbl.dialogarena.pdf;

import org.junit.Test;

import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FunctionsTest {

    @Test
    public void skalReturnere1ForEnkeltsidigPdf() throws IOException {
        byte[] pdf = getBytesFromFile("/PdfMergerFiles/skjema1_side3.pdf");
        assertThat(Functions.PDF_SIDEANTALL.transform(pdf), is(1));
    }

    @Test
    public void skalReturnere2ForTosidigPdf() throws IOException {
        byte[] pdf = getBytesFromFile("/PdfMergerFiles/skjema1_side4-5.pdf");
        assertThat(Functions.PDF_SIDEANTALL.transform(pdf), is(2));
    }
}
