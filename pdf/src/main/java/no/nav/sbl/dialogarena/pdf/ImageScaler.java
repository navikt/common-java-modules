package no.nav.sbl.dialogarena.pdf;

import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static java.lang.Math.*;

/**
 * Funksjoner for skalering av bilder
 */

public class ImageScaler {

    public enum ScaleMode {SCALE_TO_FIT_INSIDE_BOX, CROP_TO_FILL_ENTIRE_BOX}

    private static final Logger logger = LoggerFactory.getLogger(ImageScaler.class);

    public static BufferedImage scaleImage(byte[] imageBytes, Dimension boundingBox, ScaleMode scaleMode) {

        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(bais);
            return scaleImage(image, boundingBox, scaleMode);
        } catch (IOException e) {
            logger.error("Kunne ikke lese bytes som bilde under skalering.");
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage scaleImage(BufferedImage image, Dimension boundingBox, ScaleMode scaleMode) {
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
        if (boundingBox.getWidth() > image.getWidth() || boundingBox.getHeight() > image.getHeight()) {
            throw new IllegalArgumentException("Bildet må være minst like stort som boksen.");
        }
        int newWidth = (int) round(boundingBox.getWidth());
        int newHeight = (int) round(boundingBox.getHeight());

        int widthDelta = image.getWidth() - newWidth;
        int heightDelta = image.getHeight() - newHeight;

        return image.getSubimage(widthDelta / 2, heightDelta / 2, newWidth, newHeight);
    }
}
