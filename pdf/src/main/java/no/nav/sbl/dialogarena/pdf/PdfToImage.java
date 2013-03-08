package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import org.apache.commons.collections15.Transformer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static javax.imageio.ImageIO.write;
import static org.apache.pdfbox.pdmodel.PDDocument.load;


public final class PdfToImage implements Transformer<byte[], byte[]> {

//    private static final Logger logger = LoggerFactory.getLogger(PdfToImage.class);

    @Override
    public byte[] transform(byte[] bytes) {
        if (new IsImage().evaluate(bytes)) {
            return bytes;
        }

        final String filetype = "png";

        PDPage page;
        BufferedImage image;
        PDDocument document;
        try {
            document = load(new ByteArrayInputStream(bytes));
            page = (PDPage) document.getDocumentCatalog().getAllPages().get(0);
        } catch (IOException e) {
//            logger.error("Kunne ikke opprette PDF", e);
            throw new RuntimeException(e);
        }

        try {
            image = page.convertToImage();
        } catch (IOException e) {
//            logger.error("Kunne ikke konvertere første side i PDF til bilde", e);
            throw new RuntimeException(e);
        }

        try {
            document.close();
        } catch (IOException e) {
//            logger.error("Kunne ikke lukke dokumentet");
            throw new RuntimeException(e);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            write(image, filetype, bos);
        } catch (IOException e) {
//            logger.error("Kunne ikke skrive bilde til image IO", e);
            throw new RuntimeException(e);
        }
        image.flush();

        return bos.toByteArray();
    }
}