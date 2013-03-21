package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.collections15.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static javax.imageio.ImageIO.write;

class PngFromBufferedImageToByteArray implements Transformer<BufferedImage, byte[]> {

    private static final Logger logger = LoggerFactory.getLogger(PngFromBufferedImageToByteArray.class);

    @Override
    public byte[] transform(BufferedImage bufferedImage) {
        long start = System.currentTimeMillis();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            write(bufferedImage, "png", baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        bufferedImage.flush();
        byte[] bytes = baos.toByteArray();
        double elapsedTime = (double) (System.currentTimeMillis() - start) / 1000.0;
        logger.debug("Konverterte et PNG-bilde fra BufferedImage til byte[] på {} sekunder", elapsedTime);
        return bytes;
    }
}

