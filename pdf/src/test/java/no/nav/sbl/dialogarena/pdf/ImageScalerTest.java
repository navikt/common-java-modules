package no.nav.sbl.dialogarena.pdf;

import org.hamcrest.Matchers;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.writeBytesToFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ImageScalerTest {

    @Test
    public void fitImageMaintainDimensions() throws IOException {
        byte[] bytes = getBytesFromFile("/ImageScalerFiles/portrait.png");
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int frameWidth = (new Random()).nextInt(2 * image.getWidth());
        int frameHeight = (new Random()).nextInt(2 * image.getHeight());
        byte [] fittedImageBytes = ImageScaler.fitImageInsideFrame(bytes, new Dimension(frameWidth, frameHeight));
        writeBytesToFile(fittedImageBytes, "/ImageScalerFiles", "fittedImage.png");

        BufferedImage fittedImage;
        try {
            fittedImage = ImageIO.read(new ByteArrayInputStream(fittedImageBytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assertThat((double) fittedImage.getWidth() / fittedImage.getHeight(), is(Matchers.closeTo((double) image.getWidth() / image.getHeight(), 0.05)));
    }

    @Test
    public void cropImageFillsFrame() throws IOException {
        byte[] bytes = getBytesFromFile("/ImageScalerFiles/landscape.png");
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int frameWidth = (new Random()).nextInt(2 * image.getWidth());
        int frameHeight = (new Random()).nextInt(2 * image.getHeight());
        byte [] croppedImageBytes = ImageScaler.cropImageToFillFrame(bytes, new Dimension(frameWidth, frameHeight));

        BufferedImage croppedImage;
        try {
            croppedImage = ImageIO.read(new ByteArrayInputStream(croppedImageBytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertThat((double) croppedImage.getWidth(), is(Matchers.closeTo((double) frameWidth, 1.0)));
        assertThat((double) croppedImage.getHeight(), is(Matchers.closeTo((double) frameHeight, 1.0)));
    }
}
