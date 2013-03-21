package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.collections15.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Konvertererer JPG til PNG
 */

class JpgToPng implements Transformer<byte[], byte[]> {

    private static final Logger logger = LoggerFactory.getLogger(JpgToPng.class);

    @Override
    public byte[] transform(byte[] bytes) {
        long start = System.currentTimeMillis();
        BufferedImage jpgImage;
        ByteArrayOutputStream pngBaos = new ByteArrayOutputStream();

        try {
            jpgImage = ImageIO.read(new ByteArrayInputStream(bytes));
            ImageIO.write(jpgImage, "png", pngBaos);
            pngBaos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        double elapsedTime = (double) (System.currentTimeMillis() - start) / 1000.0;
        byte[] pngBytes = pngBaos.toByteArray();
        logger.debug("Konverterte et JPG-bilde til PNG på {} sekunder", elapsedTime);
        return pngBytes;
    }
}
