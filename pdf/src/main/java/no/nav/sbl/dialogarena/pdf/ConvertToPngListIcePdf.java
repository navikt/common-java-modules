package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.icepdf.core.pobjects.Document;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.cropImage;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.getPageImageFromDocument;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.setupDocumentFromBytes;

/**
 * Konvertererer PDF- og JPG-filer til liste av PNGer
 *
 * For JPG vil resultatet alltid være en liste med kun ett element.
 */

public final class ConvertToPngListIcePdf extends ConvertToPngList {

    public ConvertToPngListIcePdf(Dimension frameDimension) {
        super(frameDimension);
    }

    @Override
    public List<byte[]> transform(byte[] bytes) {
        if (new IsImage().evaluate(bytes)) {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BufferedImage scaledImage;
            try {
                scaledImage = scaleImage(ImageIO.read(bais), frameDimension);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            scaledImage = cropImage(scaledImage, frameDimension);
            List<byte[]> list = new ArrayList<>();
            list.add(new PngFromBufferedImageToByteArray().transform(scaledImage));
            return list;
        } else if (new IsPdf().evaluate(bytes)) {
            Document document = setupDocumentFromBytes(bytes);
            List<byte[]> images = new ArrayList<>();
            for(int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = getPageImageFromDocument(document, i, frameDimension);
                image = cropImage(image, frameDimension);
                images.add(new PngFromBufferedImageToByteArray().transform(image));
            }
            document.dispose();
            return images;
        } else {
            throw new IllegalArgumentException("Kan kun konvertere PDF, JPG og PNG til PNG.");
        }
    }
}