package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsImage;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.ScaleMode;
import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.getPageImageFromDocument;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.setupDocumentFromBytes;

/**
 * Konverterer PDF og JPG til PNG
 * <p/>
 * Om PDFen er flersidig blir kun forsiden konvertert til PNG.
 * Det returnerte bildet blir skalert og eventuelt croppet, i henhold
 * til konstruktørparameterne.
 */
public final class ConvertToPng implements Transformer<byte[], byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertToPng.class);
    private static final boolean GS_EXISTS;

    static {
        int exitCode = -1;
        try {
            CommandLine cmdLine = new CommandLine(decideGSCommand());
            cmdLine.addArgument("--version");
            exitCode = new DefaultExecutor().execute(cmdLine);
        } catch (IOException e) {
            LOGGER.info("Could not find ghostscript");
        }
        GS_EXISTS = exitCode == 0;
        if (GS_EXISTS) {
            LOGGER.debug("Bruker GhostScript som rendring-motor for thumbnails");
        } else {
            LOGGER.warn("Fant ikke GhostScript. Bruker PdfBox som rendring-motor for thumbnails");
        }
    }

    private final Dimension boundingBox;

    private final ScaleMode scaleMode;
    private final Integer side;

    public ConvertToPng(Dimension boundingBox, ScaleMode mode) {
        this(boundingBox, mode, 0);
    }

    public ConvertToPng(Dimension boundingBox, ScaleMode mode, Integer side) {
        this.boundingBox = boundingBox;
        this.scaleMode = mode;
        this.side = side;
    }

    @Override
    public byte[] transform(byte[] bytes) {
        if (new IsImage().evaluate(bytes)) {
            BufferedImage scaledImage = scaleImage(bytes, boundingBox, scaleMode);
            return new PngFromBufferedImageToByteArray().transform(scaledImage);
        } else if (new IsPdf().evaluate(bytes)) {
            return decidePdfTransformer().transform(bytes);
        } else {
            throw new IllegalArgumentException("Kan kun konvertere PDF, JPG og PNG til PNG.");
        }
    }

    private Transformer<byte[], byte[]> decidePdfTransformer() {
        return GS_EXISTS ? new GSTransformer() : new PdfBoxTransformer();
    }

    private class PdfBoxTransformer implements Transformer<byte[], byte[]> {

        @Override
        public byte[] transform(byte[] bytes) {
            PDDocument document = setupDocumentFromBytes(bytes);
            try {
                BufferedImage image = getPageImageFromDocument(document, side);
                image = scaleImage(image, boundingBox, scaleMode);
                return new PngFromBufferedImageToByteArray().transform(image);
            } finally {
                try {
                    document.close();
                } catch (IOException e) {
                    LOGGER.warn("Kunne ikke lukke PDF-dokument med PDFBox." + e, e);
                }
            }
        }

    }

    private class GSTransformer implements Transformer<byte[], byte[]> {

        public static final String ARGS = "-dNOPAUSE -sDEVICE=png16m -dDEVICEWIDTH=${width} -dDEVICEHEIGHT=${height} -dPDFFitPage -dFirstPage=${page} " +
                "-dLastPage=${page} -sOutputFile=%stdout%  -q -_ -c quit";

        @Override
        public byte[] transform(byte[] bytes) {
            ExecuteWatchdog watchdog = new ExecuteWatchdog(2000);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                CommandLine cmdLine = new CommandLine(decideGSCommand());
                cmdLine.addArguments(ARGS);
                cmdLine.setSubstitutionMap(genererThumbnailProperties());

                DefaultExecutor executor = new DefaultExecutor();

                executor.setStreamHandler(new PumpStreamHandler(out, System.err, bis));
                executor.setWatchdog(watchdog);

                executor.execute(cmdLine);

                return out.toByteArray();
            } catch (ExecuteException e) {
                if (watchdog.killedProcess()) {
                    throw new RuntimeException("Prosessen brukte for lang tid", e);
                } else {
                    throw new RuntimeException("Noe annet gikk galt: " + e, e);
                }
            } catch (IOException e) {
                throw new RuntimeException("Feil ved preview-generering", e);
            }
        }

        private Map<String, String> genererThumbnailProperties() {
            Map<String, String> substMap = new HashMap<>();
            substMap.put("width", "" + boundingBox.width);
            substMap.put("height", "" + boundingBox.height);
            substMap.put("page", "" + 1);
            return substMap;
        }

    }

    private static String decideGSCommand() {
        return OS.isFamilyWindows() ? "gswin32c" : "gs";
    }
}