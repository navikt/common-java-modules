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
import java.util.ArrayList;
import java.util.List;


public final class PdfToImageList implements Transformer<byte[], List<byte[]>> {

    @Override
    public List<byte[]> transform(byte[] bytes) {
        if (new IsImage().evaluate(bytes)) {
            List<byte[]> list = new ArrayList<>();
            list.add(bytes);
            return list;
        }

        Document document = new Document();
        try {
            document.setByteArray(bytes, 0, bytes.length, "foo.pdf");
        } catch (PDFException | PDFSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        float scale = 1.0f;
        float rotation = 0f;

        List<byte[]> images = new ArrayList<>();

        for(int i = 0; i < document.getNumberOfPages(); i++) {

            BufferedImage image = (BufferedImage) document.getPageImage(0, GraphicsRenderingHints.SCREEN,
                Page.BOUNDARY_CROPBOX, rotation, scale);
            images.add(new PngToByteArray().transform(image));

        }
        return images;
    }
}