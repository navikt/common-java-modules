package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.abs;
import static org.junit.Assert.assertTrue;

public class PdfTestUtils {
    public static byte[] getBytesFromFile(String path) throws IOException {
        InputStream resourceAsStream = PdfTestUtils.class.getResourceAsStream(path);
        return IOUtils.toByteArray(resourceAsStream);
    }

    public static void writeBytesToFile(byte[] bytes, String subDir, String filename) throws IOException {
        String directory = PdfTestUtils.class.getResource(subDir).getPath();
        File pdfFile = new File(directory + "/" + filename);

        FileOutputStream fos = new FileOutputStream(pdfFile);
        fos.write(bytes);
        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.isFile());
        fos.close();
    }

    public static void writeImageToFile(BufferedImage image, String subDir, String filename) throws IOException {
        byte[] bytes = new PngFromBufferedImageToByteArray().transform(image);
        writeBytesToFile(bytes, subDir, filename);
    }

    public static Matcher<? super BufferedImage> fitsInside(Dimension boundingBox) {
        return new FitsInside(boundingBox);
    }

    public static class FitsInside extends TypeSafeMatcher<BufferedImage> {

        private final Dimension boundingBox;
        private static final double EPSILON = 0.01;

        public FitsInside(Dimension boundingBox) {
            this.boundingBox = boundingBox;
        }

        @Override
        protected boolean matchesSafely(BufferedImage image) {
            boolean widthMatches = abs(image.getWidth() - boundingBox.getWidth()) < EPSILON;
            boolean heightMatches = abs(image.getHeight() - boundingBox.getHeight()) < EPSILON;
            return widthMatches || heightMatches;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("an image that fits inside the given box");
        }
    }
}
