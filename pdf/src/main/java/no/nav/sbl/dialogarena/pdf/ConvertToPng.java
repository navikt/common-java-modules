package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.icepdf.core.pobjects.Document;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.getPageImageFromDocument;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.setupDocumentFromBytes;

/**
 * Konvertererer PDF- og JPG-filer til én PNG
 *
 * For flersidig PDF blir kun forsiden konvertert til PNG og returnert.
 */

public final class ConvertToPng implements Transformer<byte[], byte[]> {

    public Dimension frameDimension;

    public ConvertToPng(Dimension frameDimension) {
        this.frameDimension = frameDimension;
    }

    @Override
    public byte[] transform(byte[] bytes) {
        if (new IsImage().evaluate(bytes)) {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BufferedImage scaledImage;
            try {
                scaledImage = scaleImage(ImageIO.read(bais), frameDimension);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new PngFromBufferedImageToByteArray().transform(scaledImage);
        } else if (new IsPdf().evaluate(bytes)) {
            Document document = setupDocumentFromBytes(bytes);

            BufferedImage image = getPageImageFromDocument(document, 0, frameDimension);

            document.dispose();
            return new PngFromBufferedImageToByteArray().transform(image);

        } else {
            throw new IllegalArgumentException("Kan kun konvertere PDF, JPG og PNG til PNG.");
        }
    }
}