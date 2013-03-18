package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.collections15.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JpgToPng implements Transformer<byte[], byte[]> {
    /*Konverterer jpeg til png*/

    private static final Logger logger = LoggerFactory.getLogger(PdfMerger.class);
    @Override
    public byte[] transform(byte[] bytes) {
        long start = System.currentTimeMillis();
        BufferedImage jpgImage;
        try {
            jpgImage = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(jpgImage, "png", baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            baos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        double elapsedTime = (double) (System.currentTimeMillis() - start) / 1000.0;
        byte[] pngBytes = baos.toByteArray();
        logger.debug("Konverterte et jpeg-bilde til png på {} sekunder", elapsedTime);
        return pngBytes;
    }
}
