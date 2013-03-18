package no.nav.sbl.dialogarena.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageToPdf implements Transformer<byte[], byte[]> {

    private static final Logger logger = LoggerFactory.getLogger(PdfMerger.class);

    @Override
    public byte[] transform(byte[] bytes) {
        if (new IsPdf().evaluate(bytes)) {
            return bytes;
        }
        ByteArrayOutputStream outputStream;
        byte[] pdfBytes;
        try {
            long start = System.currentTimeMillis();

            Image image = Image.getInstance(bytes);
            Rectangle pageSize = new Rectangle(image.getWidth(), image.getHeight());
            Document document = new Document(pageSize, 0, 0, 0, 0);
            outputStream = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();
            document.add(image);
            writer.createXmpMetadata();
            document.close();

            double elapsedTime = (double)(System.currentTimeMillis() - start) / 1000.0;
            pdfBytes = outputStream.toByteArray();
            logger.debug("Konverterte et bilde til pdf på {} sekunder", elapsedTime);
        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        }
        return pdfBytes;

    }
}
