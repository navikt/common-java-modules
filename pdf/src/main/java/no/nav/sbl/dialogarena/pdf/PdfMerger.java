package no.nav.sbl.dialogarena.pdf;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfConcatenate;
import com.itextpdf.text.pdf.PdfReader;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class PdfMerger implements Transformer<List<byte[]>, byte[]> {

    @Override
    public byte[] transform(List<byte[]> pages) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfConcatenate merger = null;
        try {
            merger = new PdfConcatenate(outputStream);
            for (byte[] page : pages) {
                if (!(new IsPdf().evaluate(page))) {
                    throw new IllegalArgumentException("All byte arrays must represent PDF files.");
                }
                PdfReader reader = new PdfReader(page);
                merger.addPages(reader);
            }

        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        } finally {
            if (merger != null) {
                merger.close();
            }
        }
        return outputStream.toByteArray();
    }
}
