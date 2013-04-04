package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.icepdf.core.pobjects.Document;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.cropImage;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.IcePdfUtils.getScaledPageImageFromDocument;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.IcePdfUtils.setupDocumentFromBytes;

/**
 * Konvertererer PDF- og JPG-filer til liste av PNGer
 *
 * For JPG vil resultatet alltid være en liste med kun ett element.
 */

public final class ConvertToPngListIcePdf implements Transformer<byte[], List<byte[]>> {

    public Dimension frameDimension;

    public ConvertToPngListIcePdf(Dimension frameDimension) {
        this.frameDimension = frameDimension;
    }

    @Override
    public List<byte[]> transform(byte[] bytes) {
        if (new IsImage().evaluate(bytes)) {
            BufferedImage scaledImage = scaleImage(bytes, frameDimension);
            scaledImage = cropImage(scaledImage, frameDimension);
            List<byte[]> list = new ArrayList<>();
            list.add(new PngFromBufferedImageToByteArray().transform(scaledImage));
            return list;
        } else if (new IsPdf().evaluate(bytes)) {
            Document document = setupDocumentFromBytes(bytes);
            List<byte[]> images = new ArrayList<>();
            for(int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = getScaledPageImageFromDocument(document, i, frameDimension);
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