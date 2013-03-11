package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import org.apache.commons.collections15.Transformer;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;

import java.awt.image.BufferedImage;
import java.io.IOException;


public final class PdfToSingleImage implements Transformer<byte[], byte[]> {

    @Override
    public byte[] transform(byte[] bytes) {
        if (new IsImage().evaluate(bytes)) {
            return bytes;
        }

        Document document = new Document();
        try {
            document.setByteArray(bytes, 0, bytes.length, "foo.pdf");
        } catch (PDFException | PDFSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        float scale = 1.0f;
        float rotation = 0f;


        BufferedImage image = (BufferedImage) document.getPageImage(0, GraphicsRenderingHints.SCREEN,
                Page.BOUNDARY_CROPBOX, rotation, scale);

        document.dispose();
        return new PngToByteArray().transform(image);
    }
}