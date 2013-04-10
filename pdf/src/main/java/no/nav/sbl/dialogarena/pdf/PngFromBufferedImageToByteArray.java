package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.collections15.Transformer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static javax.imageio.ImageIO.write;

class PngFromBufferedImageToByteArray implements Transformer<BufferedImage, byte[]> {

    @Override
    public byte[] transform(BufferedImage bufferedImage) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            write(bufferedImage, "png", stream);
            bufferedImage.flush();
            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

