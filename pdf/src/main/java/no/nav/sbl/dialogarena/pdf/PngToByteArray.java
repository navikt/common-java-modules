package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.collections15.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static javax.imageio.ImageIO.write;

public final class PngToByteArray implements Transformer<BufferedImage, byte[]> {

    private static final Logger logger = LoggerFactory.getLogger(PdfMerger.class);

    @Override
    public byte[] transform(BufferedImage bufferedImage) {
        long start = System.currentTimeMillis();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            write(bufferedImage, "png", bos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        bufferedImage.flush();
        byte[] bytes = bos.toByteArray();
        double elapsedTime = (double)(System.currentTimeMillis() - start) / 1000.0;
        logger.debug("Konverterte et png-bilde til byte[] på {} sekunder", elapsedTime);
        return bytes;
    }
}

