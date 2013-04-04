package no.nav.sbl.dialogarena.pdf;

import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Funksjoner for skalering av bilder
 */

public class ImageScaler {

    private static final Logger logger = LoggerFactory.getLogger(ImageScaler.class);

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
        byte[] croppedImageBytes = new PngFromBufferedImageToByteArray().transform(croppedImage);
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

    public static BufferedImage scaleImage(byte[] imageBytes, Dimension frameDimension) {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        BufferedImage image;
        try {
            image = ImageIO.read(bais);
        } catch (IOException e) {
            logger.error("Kunne ikke lese bytes som bilde under skalering.");
            throw new RuntimeException(e);
        }
        return scaleImage(image, frameDimension);
    }

    public static BufferedImage scaleImage(BufferedImage image, Dimension frameDimension)  {
        double scalingFactor = Math.max(frameDimension.getWidth() / image.getWidth(), frameDimension.getHeight() / image.getHeight());
        Dimension dimension = new Dimension();
        dimension.setSize(scalingFactor * image.getWidth(), scalingFactor * image.getHeight());
        return Scalr.resize(image, (int) dimension.getWidth(), (int) dimension.getHeight());
    }

    public static float getScalingFactor(Dimension pageDimension, Dimension frameDimension) {
        return (float) Math.max(frameDimension.getWidth() / pageDimension.getWidth(),
                frameDimension.getHeight() / pageDimension.getHeight());
    }

    public static BufferedImage cropImage(BufferedImage image, Dimension frameDimension) {
        int width = Math.min(image.getWidth(), (int) Math.round(frameDimension.getWidth()));
        int height = Math.min(image.getHeight(), (int) Math.round(frameDimension.getHeight()));
        return image.getSubimage(0, 0, width, height);
    }
}
