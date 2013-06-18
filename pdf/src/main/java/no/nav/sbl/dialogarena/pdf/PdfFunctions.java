package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.pdf.PdAutoCloseable.PDDocumentAutoCloseable;
import org.apache.commons.collections15.Transformer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.PdAutoCloseable.autoClose;

/**
 * Funksjoner for operasjoner på PDFer, inkl henting av antall sider
 */
public final class PdfFunctions {
    private static final Logger LOG = LoggerFactory.getLogger(PdfFunctions.class);

    public static final Transformer<byte[], Integer> PDF_SIDEANTALL = new Transformer<byte[], Integer>() {
        @Override
        public Integer transform(byte[] pdfdata) {
            try (PDDocumentAutoCloseable pd = autoClose(PDDocument.load(new ByteArrayInputStream(pdfdata)))){
                return pd.document.getNumberOfPages();
            } catch (IOException e) {
                LOG.warn("Could not get number of pages from pdf document: " + e.getMessage());
                LOG.debug(e.getMessage(), e);
                return 0;
            }
        }
    };

    private PdfFunctions() { }
}
