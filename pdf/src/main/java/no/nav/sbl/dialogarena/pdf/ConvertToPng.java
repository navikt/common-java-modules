package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.ScaleMode;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.getPageImageFromDocument;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.setupDocumentFromBytes;

/**
 * Konverterer PDF og JPG til PNG
 * <p/>
 * Om PDFen er flersidig blir kun forsiden konvertert til PNG.
 * Det returnerte bildet blir skalert og eventuelt croppet, i henhold
 * til konstruktørparameterne.
 */
public final class ConvertToPng implements Transformer<byte[], byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertToPng.class);

    private final Dimension boundingBox;
    private final ScaleMode scaleMode;
    private final Integer side;

    public ConvertToPng(Dimension boundingBox, ScaleMode mode, Integer side) {
        this.boundingBox = boundingBox;
        this.scaleMode = mode;
        this.side = side;
    }

    @Override
    public byte[] transform(byte[] bytes) {
        if (new IsImage().evaluate(bytes)) {
            BufferedImage scaledImage = scaleImage(bytes, boundingBox, scaleMode);
            return new PngFromBufferedImageToByteArray().transform(scaledImage);
        } else if (new IsPdf().evaluate(bytes)) {
            PDDocument document = setupDocumentFromBytes(bytes);
            try {
                BufferedImage image = getPageImageFromDocument(document, side);
                image = scaleImage(image, boundingBox, scaleMode);
                return new PngFromBufferedImageToByteArray().transform(image);
            } finally {
                try {
                    document.close();
                } catch (IOException e) {
                    LOGGER.warn("Kunne ikke lukke PDF-dokument med PDFBox." + e, e);
                }
            }
        } else {
            throw new IllegalArgumentException("Kan kun konvertere PDF, JPG og PNG til PNG.");
        }
    }
}