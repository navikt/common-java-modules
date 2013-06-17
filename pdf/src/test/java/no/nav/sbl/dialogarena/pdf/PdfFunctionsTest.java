package no.nav.sbl.dialogarena.pdf;

import org.junit.Test;

import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PdfFunctionsTest {

    @Test
    public void skalReturnere1ForEnkeltsidigPdf() throws IOException {
        byte[] pdf = getBytesFromFile("/PdfMergerFiles/skjema1_side3.pdf");
        assertThat(PdfFunctions.PDF_SIDEANTALL.transform(pdf), is(1));
    }

    @Test
    public void skalReturnere2ForTosidigPdf() throws IOException {
        byte[] pdf = getBytesFromFile("/PdfMergerFiles/skjema1_side4-5.pdf");
        assertThat(PdfFunctions.PDF_SIDEANTALL.transform(pdf), is(2));
    }

    @Test
    public void skalReturnere0ForDataSomIkkeErPdf() {
        assertThat(PdfFunctions.PDF_SIDEANTALL.transform(new byte[1]), is(0));
    }
}
