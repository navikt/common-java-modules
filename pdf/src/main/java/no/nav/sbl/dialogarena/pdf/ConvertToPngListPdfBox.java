package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static javax.imageio.ImageIO.write;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.cropImage;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.PdfBoxUtils.setupDocumentFromBytes;


public final class ConvertToPngListPdfBox extends ConvertToPngList {

    private static final Logger logger = LoggerFactory.getLogger(ConvertToPngListPdfBox.class);

    public ConvertToPngListPdfBox(Dimension frameDimension) {
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
            java.util.List<byte[]> list = new ArrayList<>();
            list.add(new PngFromBufferedImageToByteArray().transform(scaledImage));
            return list;
        } else if (new IsPdf().evaluate(bytes)) {
            final String filetype = "png";

            BufferedImage image;
            List<byte[]> images = new ArrayList<>();
            PDDocument document = setupDocumentFromBytes(bytes);
            List<PDPage> pages = document.getDocumentCatalog().getAllPages();
            for(PDPage page : pages) {
                try {
                    image = page.convertToImage();
                } catch (IOException e) {
                    logger.error("Kunne ikke konvertere PDF-side til bilde med PDFBox", e);
                    throw new RuntimeException(e);
                }
                image = scaleImage(image, frameDimension);
                image = cropImage(image, frameDimension);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    write(image, filetype, bos);
                } catch (IOException e) {
                    logger.error("Kunne ikke skrive bilde til image IO", e);
                    throw new RuntimeException(e);
                }
                image.flush();

                images.add(bos.toByteArray());

            }
            return images;
        } else {
            throw new IllegalArgumentException("Kan kun konvertere PDF, JPG og PNG til PNG.");
        }


    }
}