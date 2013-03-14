package no.nav.sbl.dialogarena.pdf;

import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class TransformerUtils {
    /*Grupperer funksjoner som brukes i KonverterForsideTilPng og KonverterAlleSiderTilPng slik at det ikke er duplisert kode*/
    public static Document setupDocumentFromBytes(byte[] bytes) {
        Document document = new Document();
        try {
            document.setByteArray(bytes, 0, bytes.length, "foo.pdf");
            return document;
        } catch (PDFException | PDFSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage getPageImageFromDocument(Document document, int pageNumber) {
        float rotation = 0f;
        float scale = 1.0f;
        return (BufferedImage) document.getPageImage(pageNumber, GraphicsRenderingHints.SCREEN,
                Page.BOUNDARY_CROPBOX, rotation, scale);
    }

}
