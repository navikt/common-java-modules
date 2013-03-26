package no.nav.sbl.dialogarena.pdf;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.cropImage;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.getScalingFactor;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

public class ImageScalerTest {

    @Test
    public void cropImageReturnsImageWithRightDimensions() throws IOException {
        byte[] bytes = getBytesFromFile("/ImageScalerFiles/landscape.png");
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        BufferedImage image = ImageIO.read(bais);
        Dimension frame = new Dimension(50, 75);
        BufferedImage croppedImage = cropImage(image, frame);
        assertThat(croppedImage.getWidth(), is((int) frame.getWidth()));
        assertThat(croppedImage.getHeight(), is((int) frame.getHeight()));
    }

    @Test
    public void scaleImageMaintainsDimensions() throws IOException {
        byte[] bytes = getBytesFromFile("/ImageScalerFiles/portrait.png");
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        BufferedImage image = ImageIO.read(bais);
        Dimension frame = new Dimension(100, 150);
        BufferedImage scaledImage = scaleImage(image, frame);
        assertThat((double) scaledImage.getWidth() / image.getWidth(), is(closeTo((double) scaledImage.getHeight() / image.getHeight(), 0.01)));
    }

    @Test
    public void getScalingFactorWorks() {
        Dimension page = new Dimension(100, 200);
        Dimension frame = new Dimension(200, 100);
        float scalingFactor = getScalingFactor(page, frame);
        assertThat(scalingFactor, equalTo(2f));
        page.setSize(200.0, 100.0);
        scalingFactor = getScalingFactor(page, frame);
        assertThat(scalingFactor, equalTo(1f));
    }

}
