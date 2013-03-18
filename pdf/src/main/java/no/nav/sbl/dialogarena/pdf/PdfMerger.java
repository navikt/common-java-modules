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

public class PdfMerger implements Transformer<Iterable<byte[]>, byte[]> {

    private static final Logger logger = LoggerFactory.getLogger(PdfMerger.class);

    @Override
    public byte[] transform(Iterable<byte[]> pages) {
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
            double elapsedTime = (System.currentTimeMillis() - start) / 1000.0;
            logger.debug("Merget {} pdf-dokumenter på {} sekunder", numberOfMergedDocuments, elapsedTime);
            if (merger != null) {
                merger.close();
            }
        }
        return outputStream.toByteArray();
    }
}
