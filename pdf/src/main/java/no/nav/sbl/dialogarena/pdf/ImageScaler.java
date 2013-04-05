package no.nav.sbl.dialogarena.pdf;

import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

/**
 * Funksjoner for skalering av bilder
 */

public class ImageScaler {

    public enum ScaleMode { SCALE_TO_FIT_INSIDE_BOX, CROP_TO_FILL_ENTIRE_BOX }

    private static final Logger logger = LoggerFactory.getLogger(ImageScaler.class);

    public static BufferedImage scaleImage(byte[] imageBytes, Dimension boundingBox, ScaleMode scaleMode) {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        BufferedImage image;
        try {
            image = ImageIO.read(bais);
        } catch (IOException e) {
            logger.error("Kunne ikke lese bytes som bilde under skalering.");
            throw new RuntimeException(e);
        }
        return scaleImage(image, boundingBox, scaleMode);
    }

    public static BufferedImage scaleImage(BufferedImage image, Dimension boundingBox, ScaleMode scaleMode)  {
        double scaleFactorWidth = boundingBox.getWidth() / image.getWidth();
        double scaleFactorHeight = boundingBox.getHeight() / image.getHeight();

        double scalingFactor;
        if (scaleMode == ScaleMode.SCALE_TO_FIT_INSIDE_BOX) {
            scalingFactor = min(scaleFactorWidth, scaleFactorHeight);
        } else {
            scalingFactor = max(scaleFactorWidth, scaleFactorHeight);
        }

        BufferedImage scaledImage = Scalr.resize(image, (int) (scalingFactor * image.getWidth()), (int) (scalingFactor * image.getHeight()));

        if (scaleMode == ScaleMode.CROP_TO_FILL_ENTIRE_BOX) {
            return cropImage(scaledImage, boundingBox);
        } else {
            return scaledImage;
        }
    }

    public static BufferedImage cropImage(BufferedImage image, Dimension boundingBox) {
        int width = min(image.getWidth(), (int) round(boundingBox.getWidth()));
        int height = min(image.getHeight(), (int) round(boundingBox.getHeight()));
        // TODO Sentrer
        return image.getSubimage(0, 0, width, height);
    }
}
