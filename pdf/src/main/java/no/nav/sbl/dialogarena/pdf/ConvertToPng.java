package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsJpg;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.detect.IsPng;
import org.apache.commons.collections15.Transformer;
import org.icepdf.core.pobjects.Document;

import java.awt.image.BufferedImage;

import static no.nav.sbl.dialogarena.pdf.TransformerUtils.getPageImageFromDocument;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.setupDocumentFromBytes;

/**
 * Konvertererer PDF- og JPG-filer til én PNG
 *
 * For flersidig PDF blir kun forsiden konvertert til PNG og returnert.
 */

public final class ConvertToPng implements Transformer<byte[], byte[]> {

    @Override
    public byte[] transform(byte[] bytes) {
        if (new IsPng().evaluate(bytes)) {
            return bytes;
        } else if (new IsJpg().evaluate(bytes)) {
            return new JpgToPng().transform(bytes);
        } else if (new IsPdf().evaluate(bytes)) {
            Document document = setupDocumentFromBytes(bytes);

            BufferedImage image = getPageImageFromDocument(document, 0);

            document.dispose();
            return new PngFromBufferedImageToByteArray().transform(image);

        } else {
            throw new IllegalArgumentException("Kan kun konvertere PDF, JPG og PNG til PNG.");
        }
    }
}