package no.nav.sbl.dialogarena.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


class TransformerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformerUtils.class);

    public static PDDocument setupDocumentFromBytes(byte[] bytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            return PDDocument.load(inputStream);
        } catch (IOException e) {
            LOGGER.error("Kunne ikke opprette PDF-dokument fra byte array med PDFBox.", e);
            throw new RuntimeException(e);
        }
    }

    public static PDDocument setupDocumentFromInputStream(InputStream bytes) {
        try {
            return PDDocument.load(bytes);
        } catch (IOException e) {
            LOGGER.error("Kunne ikke opprette PDF-dokument fra byte array med PDFBox.", e);
            throw new RuntimeException(e);
        } finally {
            try {
                bytes.close();
            } catch (IOException ignore) {
                LOGGER.debug("Failed to close document");
            }
        }
    }

    public static BufferedImage getPageImageFromDocument(PDDocument document, int pageNumber) {
        BufferedImage image;
        try {
            PDPage page = (PDPage) document.getDocumentCatalog().getAllPages().get(pageNumber);
            image = page.convertToImage();
        } catch (IOException e) {
            LOGGER.error("Kunne ikke hente ut PDF-side fra PDF-dokument med PDFBox.", e);
            throw new RuntimeException(e);
        }
        return image;
    }
}
