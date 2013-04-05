package no.nav.sbl.dialogarena.pdf;

import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.ScaleMode.CROP_TO_FILL_ENTIRE_BOX;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.ScaleMode.SCALE_TO_FIT_INSIDE_BOX;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.cropImage;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.fitsInside;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.writeImageToFile;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

public class ImageScalerTest {

    private static final String IMAGE_DIR = "/ImageScalerFiles";
    private static BufferedImage image;
    private static Dimension boundingBox;

    @Before
    public void setup() throws IOException {
        byte[] bytes = getBytesFromFile(IMAGE_DIR + "/landscape.png");
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        image = ImageIO.read(bais);
        boundingBox = new Dimension(100, 150);
    }

    @Test
    public void scaleImageToFitBoxMakesImageFitInsideBox() {
        BufferedImage imageScaledToFitBox = scaleImage(image, boundingBox, SCALE_TO_FIT_INSIDE_BOX);
        assertThat(imageScaledToFitBox, fitsInside(boundingBox));
    }

    @Test
    public void scaleImageToFitBoxMaintainsAspectRatio() throws IOException {
        BufferedImage imageScaledToFitBox = scaleImage(image, boundingBox, SCALE_TO_FIT_INSIDE_BOX);

        double scaleFactorWidth = imageScaledToFitBox.getWidth() / image.getWidth();
        double scaleFactorHeight = imageScaledToFitBox.getHeight() / image.getHeight();
        assertThat(scaleFactorWidth, is(closeTo(scaleFactorHeight, 0.01)));

        writeImageToFile(imageScaledToFitBox, IMAGE_DIR, "scaledImage.png");
    }

    @Test
    public void cropImageToFillBoxMaintainsDimensions() throws IOException {
        BufferedImage imageCroppedToFillBox = scaleImage(image, boundingBox, CROP_TO_FILL_ENTIRE_BOX);
        assertThat(imageCroppedToFillBox.getWidth(), is((int) boundingBox.getWidth()));
        assertThat(imageCroppedToFillBox.getHeight(), is((int) boundingBox.getHeight()));

        writeImageToFile(imageCroppedToFillBox, IMAGE_DIR, "croppedImage.png");
    }

    @Test
    public void cropImageReturnsImageWithRightDimensions() throws IOException {
        byte[] bytes = getBytesFromFile("/ImageScalerFiles/landscape.png");
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        BufferedImage landscapeImage = ImageIO.read(bais);
        Dimension frame = new Dimension(50, 75);
        BufferedImage croppedImage = cropImage(landscapeImage, frame);
        assertThat(croppedImage.getWidth(), is((int) frame.getWidth()));
        assertThat(croppedImage.getHeight(), is((int) frame.getHeight()));
    }
}
