package no.nav.sbl.dialogarena.pdf;


import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageScaler {
    /*Funksjoner for skalering av bilder*/

    public static byte[] cropImageToFillFrame(byte[] imageBytes, Dimension frameDimension) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BufferedImage croppedImage;

        float widthZoom = (float) frameDimension.getWidth() / image.getWidth();
        float heightZoom = (float) frameDimension.getHeight() / image.getHeight();
        if (widthZoom > heightZoom) {
            croppedImage = image.getSubimage(0, 0, image.getWidth(), Math.round(image.getHeight() * (heightZoom / widthZoom)));
        } else {
            croppedImage = image.getSubimage(0, 0, Math.round(image.getWidth() * (widthZoom / heightZoom)), image.getHeight());
        }
        byte[] croppedImageBytes = new PngToByteArray().transform(croppedImage);
        return fitImageInsideFrame(croppedImageBytes, frameDimension);
    }

    public static byte[] fitImageInsideFrame(byte[] imageBytes, Dimension frameDimension) {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedImage scaledImage;
        try {
            scaledImage = Scalr.resize(ImageIO.read(bais), (int) frameDimension.getWidth(), (int) frameDimension.getHeight());
            ImageIO.write(scaledImage, "png", baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
}
