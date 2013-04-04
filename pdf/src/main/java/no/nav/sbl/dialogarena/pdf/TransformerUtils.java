package no.nav.sbl.dialogarena.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PDimension;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.getScalingFactor;

class TransformerUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConvertToPngListPdfBox.class);

    static class IcePdfUtils {
        public static Document setupDocumentFromBytes(byte[] bytes) {
            Document document = new Document();
            try {
                document.setByteArray(bytes, 0, bytes.length, "foo.pdf");
                return document;
            } catch (PDFException | PDFSecurityException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static BufferedImage getPageImageFromDocument(Document document, int pageNumber, Dimension frameDimension) {
            PDimension pageDimension = document.getPageDimension(pageNumber, 0f);
            float scale = getScalingFactor(new Dimension((int) Math.ceil(pageDimension.getWidth()), (int) Math.ceil(pageDimension.getHeight())), frameDimension);
            return (BufferedImage) document.getPageImage(pageNumber, GraphicsRenderingHints.SCREEN,
                    Page.BOUNDARY_CROPBOX, 0f, scale);
        }
    }

    static class PdfBoxUtils {

        public static PDDocument setupDocumentFromBytes(byte[] bytes) {
            try {
                return PDDocument.load(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                logger.error("Kunne ikke opprette PDF fra byte array med PDFBox.", e);
                throw new RuntimeException(e);
            }
        }

    }
}
