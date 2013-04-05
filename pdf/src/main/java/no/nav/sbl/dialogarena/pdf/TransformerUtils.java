package no.nav.sbl.dialogarena.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;

class TransformerUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConvertToPngList.class);

    public static PDDocument setupDocumentFromBytes(byte[] bytes) {
        try {
            return PDDocument.load(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            logger.error("Kunne ikke opprette PDF fra byte array med PDFBox.", e);
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage getScaledPageImageFromDocument(PDDocument document, int pageNumber, Dimension frameDimension) {
        BufferedImage image;
        try {
            PDPage page = (PDPage) document.getDocumentCatalog().getAllPages().get(pageNumber);
            image = page.convertToImage();
        } catch (IOException e) {
            logger.error("Kunne ikke opprette PDF", e);
            throw new RuntimeException(e);
        }

        return scaleImage(image, frameDimension);
    }
}
