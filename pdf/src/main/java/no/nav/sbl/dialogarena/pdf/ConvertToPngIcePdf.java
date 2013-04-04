package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.icepdf.core.pobjects.Document;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.cropImage;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.IcePdfUtils.getScaledPageImageFromDocument;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.IcePdfUtils.setupDocumentFromBytes;

/**
 * Konvertererer PDF- og JPG-filer til én PNG
 *
 * For flersidig PDF blir kun forsiden konvertert til PNG og returnert.
 */

public final class ConvertToPngIcePdf implements Transformer<byte[], byte[]> {

    public Dimension frameDimension;

    public ConvertToPngIcePdf(Dimension frameDimension) {
        this.frameDimension = frameDimension;
    }

    @Override
    public byte[] transform(byte[] bytes) {
        if (new IsImage().evaluate(bytes)) {
            BufferedImage scaledImage = scaleImage(bytes, frameDimension);
            scaledImage = cropImage(scaledImage, frameDimension);
            return new PngFromBufferedImageToByteArray().transform(scaledImage);
        } else if (new IsPdf().evaluate(bytes)) {
            Document document = setupDocumentFromBytes(bytes);
            BufferedImage image = getScaledPageImageFromDocument(document, 0, frameDimension);
            image = cropImage(image, frameDimension);
            document.dispose();
            return new PngFromBufferedImageToByteArray().transform(image);

        } else {
            throw new IllegalArgumentException("Kan kun konvertere PDF, JPG og PNG til PNG.");
        }
    }
}