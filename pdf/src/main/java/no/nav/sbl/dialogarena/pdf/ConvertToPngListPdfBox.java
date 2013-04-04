package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.cropImage;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.PdfBoxUtils.getScaledPageImageFromDocument;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.PdfBoxUtils.setupDocumentFromBytes;


public final class ConvertToPngListPdfBox implements Transformer<byte[], List<byte[]>> {

    public Dimension frameDimension;

    public ConvertToPngListPdfBox(Dimension frameDimension) {
        this.frameDimension = frameDimension;
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
            java.util.List<byte[]> list = new ArrayList<>();
            list.add(new PngFromBufferedImageToByteArray().transform(scaledImage));
            return list;
        } else if (new IsPdf().evaluate(bytes)) {
            List<byte[]> images = new ArrayList<>();
            PDDocument document = setupDocumentFromBytes(bytes);
            int numberOfPages = document.getDocumentCatalog().getAllPages().size();
            for(int i = 0; i < numberOfPages; i++) {
                BufferedImage image = getScaledPageImageFromDocument(document, i, frameDimension);
                image = cropImage(image, frameDimension);
                images.add(new PngFromBufferedImageToByteArray().transform(image));
            }
            return images;
        } else {
            throw new IllegalArgumentException("Kan kun konvertere PDF, JPG og PNG til PNG.");
        }


    }
}