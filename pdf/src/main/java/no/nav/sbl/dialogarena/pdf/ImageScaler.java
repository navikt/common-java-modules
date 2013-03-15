package no.nav.sbl.dialogarena.pdf;

import java.awt.Dimension;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageScaler {
    /*Funksjoner for skalering av bilder*/

    public byte[] cropImageToFillFrame(byte[] imageBytes, Dimension frameDimension) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        float zoom = Math.max(
                (float) frameDimension.getWidth() / image.getWidth(),
                (float) frameDimension.getHeight() / image.getHeight());
        BufferedImage croppedImage = image.getSubimage(0, 0, Math.round(zoom * image.getWidth()), Math.round(zoom * image.getHeight()));
        return new PngToByteArray().transform(croppedImage);
    }

    public byte[] fitImageInsideFrame(byte[] imageBytes, Dimension frameDimension) {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedImage scaledImage;
        try {
            scaledImage = Scalr.resize(ImageIO.read(bais), (int) frameDimension.getWidth(), (int) frameDimension.getHeight());
            ImageIO.write(scaledImage, "jpg", baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
}
