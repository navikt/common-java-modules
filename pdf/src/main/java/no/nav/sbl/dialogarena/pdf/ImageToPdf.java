package no.nav.sbl.dialogarena.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import no.nav.sbl.dialogarena.detect.IsJpg;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.detect.IsPng;
import org.apache.commons.collections15.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Konverterer PNG og JPG til PDF.
 */

public class ImageToPdf implements Transformer<byte[], byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageToPdf.class);

    @Override
    public byte[] transform(byte[] bytes) {
        if (new IsPdf().evaluate(bytes)) {
            return bytes;
        } else if (new IsPng().evaluate(bytes) || new IsJpg().evaluate(bytes)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] pdfBytes;
            long start = System.currentTimeMillis();
            Document document;
            try {
                Image image = Image.getInstance(bytes);
                Rectangle pageSize = new Rectangle(image.getWidth(), image.getHeight());
                document = new Document(pageSize, 0, 0, 0, 0);
                PdfWriter writer = PdfWriter.getInstance(document, outputStream);
                document.open();
                document.add(image);
                writer.createXmpMetadata();
            } catch (IOException | DocumentException e) {
                throw new RuntimeException(e);
            }
            document.close();
            double tusen = 1000.0;
            double elapsedTime = (double)(System.currentTimeMillis() - start) / tusen;
            pdfBytes = outputStream.toByteArray();
            LOGGER.debug("Konverterte et bilde til PDF på {} sekunder", elapsedTime);
            return pdfBytes;
        } else {
            throw new IllegalArgumentException("Kan kun konvertere JPG, PNG og PDF til PDF.");
        }

    }
}
