package no.nav.sbl.dialogarena.pdf;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfConcatenate;
import com.itextpdf.text.pdf.PdfReader;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Slår sammen flere PDF-filer til én enkelt PDF.
 */

public class PdfMerger implements Transformer<Iterable<byte[]>, byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfMerger.class);

    @Override
    public final byte[] transform(Iterable<byte[]> pages) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfConcatenate merger = null;
        long start = System.currentTimeMillis();
        int numberOfMergedDocuments = 0;
        try {
            merger = new PdfConcatenate(outputStream);
            for (byte[] page : pages) {
                if (!(new IsPdf().evaluate(page))) {
                    throw new IllegalArgumentException("All byte arrays must represent PDF files.");
                }
                PdfReader reader = new PdfReader(page);
                merger.addPages(reader);
                numberOfMergedDocuments++;
            }

        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        } finally {
            final double tusen = 1000.0;
            double elapsedTime = (System.currentTimeMillis() - start) / tusen;
            LOGGER.debug("Merget {} PDF-dokumenter på {} sekunder", numberOfMergedDocuments, elapsedTime);
            if (merger != null) {
                merger.close();
            }
        }
        return outputStream.toByteArray();
    }
}
