package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsJpg;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.detect.IsPng;
import org.apache.commons.collections15.Transformer;
import org.icepdf.core.pobjects.Document;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static no.nav.sbl.dialogarena.pdf.TransformerUtils.getPageImageFromDocument;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.setupDocumentFromBytes;


public final class ConvertToPngList implements Transformer<byte[], List<byte[]>> {

    @Override
    public List<byte[]> transform(byte[] bytes) {
        if (new IsPng().evaluate(bytes)) {
            List<byte[]> list = new ArrayList<>();
            list.add(bytes);
            return list;
        }

        else if (new IsJpg().evaluate(bytes)) {
            List<byte[]> list = new ArrayList<>();
            list.add(new JpgToPng().transform(bytes));
            return list;
        }

        else if (new IsPdf().evaluate(bytes)) {
            Document document = setupDocumentFromBytes(bytes);

            List<byte[]> images = new ArrayList<>();

            for(int i = 0; i < document.getNumberOfPages(); i++) {

                BufferedImage image = getPageImageFromDocument(document, i);
                images.add(new PngToByteArray().transform(image));

            }
            return images;
        }

        else {
            throw new IllegalArgumentException("Støtter bare png, jpeg og pdf");
        }
    }
}