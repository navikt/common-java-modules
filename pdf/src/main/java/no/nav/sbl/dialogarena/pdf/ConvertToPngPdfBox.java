package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static javax.imageio.ImageIO.write;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.cropImage;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;


public final class ConvertToPngPdfBox extends ConvertToPng {

    private static final Logger logger = LoggerFactory.getLogger(ConvertToPngPdfBox.class);

    public ConvertToPngPdfBox(Dimension frameDimension) {
        super(frameDimension);
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
            scaledImage = cropImage(scaledImage, frameDimension);
            return new PngFromBufferedImageToByteArray().transform(scaledImage);
        } else if (new IsPdf().evaluate(bytes)) {
            final String filetype = "png";

            BufferedImage image;
            PDDocument document;
            try {
                document = PDDocument.load(new ByteArrayInputStream(bytes));
                document.getDocumentCatalog().getAllPages().get(0);
                PDPage page = (PDPage) document.getDocumentCatalog().getAllPages().get(0);
                image = page.convertToImage();
            } catch (IOException e) {
                logger.error("Kunne ikke opprette PDF", e);
                throw new RuntimeException(e);
            }

            image = scaleImage(image, frameDimension);
            image = cropImage(image, frameDimension);

            try {
                document.close();
            } catch (IOException e) {
                logger.error("Kunne ikke lukke dokumentet");
                throw new RuntimeException(e);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                write(image, filetype, bos);
            } catch (IOException e) {
                logger.error("Kunne ikke skrive bilde til image IO", e);
                throw new RuntimeException(e);
            }
            image.flush();

            return bos.toByteArray();
        } else {
            throw new IllegalArgumentException("Kan kun konvertere PDF, JPG og PNG til PNG.");
        }


    }
}