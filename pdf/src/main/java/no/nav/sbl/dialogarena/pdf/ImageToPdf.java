package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsJpg;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.detect.IsPng;
import org.apache.commons.collections15.Transformer;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDPixelMap;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
            PDDocument pdDocument = null;
            try {
                pdDocument = new PDDocument();
                PDXObjectImage image = getImage(pdDocument, bytes);

                PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
                pdDocument.addPage(page);

                PDPageContentStream is = new PDPageContentStream(pdDocument, page);
                is.drawImage(image, 0, 0);
                is.close();
                pdDocument.save(outputStream);

            } catch (IOException | COSVisitorException e) {
                throw new RuntimeException(e);
            } finally {
                if (pdDocument != null) {
                    try {
                        pdDocument.close();
                    } catch (IOException ignore) {
                    }
                }
            }
            final double tusen = 1000.0;
            double elapsedTime = (double) (System.currentTimeMillis() - start) / tusen;
            pdfBytes = outputStream.toByteArray();
            LOGGER.debug("Konverterte et bilde til PDF på {} sekunder", elapsedTime);
            return pdfBytes;
        } else {
            throw new IllegalArgumentException("Kan kun konvertere JPG, PNG og PDF til PDF.");
        }

    }

    private PDXObjectImage getImage(PDDocument document, byte[] bytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (new IsPng().evaluate(bytes)) {
            return new PDPixelMap(document, image);
        } else if (new IsJpg().evaluate(bytes)) {
            return new PDJpeg(document, image);
        }
        return null;
    }
}
