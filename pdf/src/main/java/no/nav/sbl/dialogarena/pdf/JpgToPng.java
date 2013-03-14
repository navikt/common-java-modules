package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.collections15.Transformer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JpgToPng implements Transformer<byte[], byte[]> {
    /*Konverterer jpeg til png*/

    @Override
    public byte[] transform(byte[] bytes) {
        BufferedImage jpgImage = null;
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
        return baos.toByteArray();
    }
}
