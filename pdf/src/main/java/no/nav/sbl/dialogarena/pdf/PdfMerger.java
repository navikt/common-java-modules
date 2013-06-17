package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
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
        PDFMergerUtility newMerger = new PDFMergerUtility();
        long start = System.currentTimeMillis();
        int numberOfMergedDocuments = 0;
        try {
            for (byte[] page : pages) {
                if (!(new IsPdf().evaluate(page))) {
                    throw new IllegalArgumentException("All byte arrays must represent PDF files.");
                }
                newMerger.addSource(new ByteArrayInputStream(page));
                numberOfMergedDocuments++;
            }
            newMerger.setDestinationStream(outputStream);
            newMerger.mergeDocuments();
        } catch (IOException|COSVisitorException e) {
            throw new RuntimeException(e);
        } finally {
            final double tusen = 1000.0;
            double elapsedTime = (System.currentTimeMillis() - start) / tusen;
            LOGGER.debug("Merget {} PDF-dokumenter på {} sekunder", numberOfMergedDocuments, elapsedTime);
        }
        return outputStream.toByteArray();
    }
}
