package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.collections15.Transformer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static javax.imageio.ImageIO.write;

public final class PngToByteArray implements Transformer<BufferedImage, byte[]> {

    @Override
    public byte[] transform(BufferedImage bufferedImage) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            write(bufferedImage, "png", bos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        bufferedImage.flush();
        return bos.toByteArray();
    }
}

