package no.nav.sbl.dialogarena.pdf;

import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PDimension;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.getScalingFactor;

class TransformerUtils {
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
