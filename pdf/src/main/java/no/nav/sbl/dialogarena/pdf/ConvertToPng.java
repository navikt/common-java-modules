package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.ScaleMode;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.getPageImageFromDocument;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.setupDocumentFromBytes;

/**
 * Konverterer PDF og JPG til PNG
 *
 * Om PDFen er flersidig blir kun forsiden konvertert til PNG.
 * Det returnerte bildet blir skalert og eventuelt croppet, i henhold
 * til konstruktørparameterne.
 */
public final class ConvertToPng implements Transformer<byte[], byte[]> {

    private final Dimension boundingBox;
    private final ScaleMode scaleMode;

    public ConvertToPng(Dimension boundingBox, ScaleMode mode) {
        this.boundingBox = boundingBox;
        this.scaleMode = mode;
    }

    @Override
    public byte[] transform(byte[] bytes) {
        if (new IsImage().evaluate(bytes)) {
            BufferedImage scaledImage = scaleImage(bytes, boundingBox, scaleMode);
            return new PngFromBufferedImageToByteArray().transform(scaledImage);
        } else if (new IsPdf().evaluate(bytes)) {
            PDDocument document = setupDocumentFromBytes(bytes);
            BufferedImage image = getPageImageFromDocument(document, 0);
            image = scaleImage(image, boundingBox, scaleMode);
            return new PngFromBufferedImageToByteArray().transform(image);
        } else {
            throw new IllegalArgumentException("Kan kun konvertere PDF, JPG og PNG til PNG.");
        }
    }
}