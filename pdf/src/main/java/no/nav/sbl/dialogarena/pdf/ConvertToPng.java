package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.cropImage;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.getScaledPageImageFromDocument;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.setupDocumentFromBytes;


public final class ConvertToPng implements Transformer<byte[], byte[]> {

    public Dimension frameDimension;

    public ConvertToPng(Dimension frameDimension) {
        this.frameDimension = frameDimension;
    }

    @Override
    public byte[] transform(byte[] bytes) {
        if (new IsImage().evaluate(bytes)) {
            BufferedImage scaledImage = scaleImage(bytes, frameDimension);
            scaledImage = cropImage(scaledImage, frameDimension);
            return new PngFromBufferedImageToByteArray().transform(scaledImage);
        } else if (new IsPdf().evaluate(bytes)) {
            PDDocument document = setupDocumentFromBytes(bytes);
            BufferedImage image = getScaledPageImageFromDocument(document, 0, frameDimension);
            image = cropImage(image, frameDimension);
            return new PngFromBufferedImageToByteArray().transform(image);
        } else {
            throw new IllegalArgumentException("Kan kun konvertere PDF, JPG og PNG til PNG.");
        }


    }
}