package no.nav.sbl.dialogarena.pdf;


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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static no.nav.sbl.dialogarena.pdf.ImageScaler.scaleImage;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.getPageImageFromDocument;
import static no.nav.sbl.dialogarena.pdf.TransformerUtils.setupDocumentFromInputStream;

public class PdfToPng implements Transformer<InputStream, byte[]> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfToPng.class);
    public static final int TIMEOUT_PAA_GENERERING = 4000;
    public static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    private static final boolean GS_EXISTS = checkIfGhostScriptExists();


    private final Dimension boundingBox;
    private final ImageScaler.ScaleMode scaleMode;
    private final Integer side;

    public PdfToPng(Dimension boundingBox, ImageScaler.ScaleMode mode, int side) {
        this.boundingBox = boundingBox;
        this.scaleMode = mode;
        this.side = side;
    }

    @Override
    public byte[] transform(InputStream inputStream) {
        return decidePdfTransformer().transform(inputStream);
    }

    private Transformer<InputStream, byte[]> decidePdfTransformer() {
        return GS_EXISTS ? new GSTransformer() : new PdfBoxTransformer();
    }

    private class PdfBoxTransformer implements Transformer<InputStream, byte[]> {

        @Override
        public byte[] transform(InputStream inputStream) {
            PDDocument document = setupDocumentFromInputStream(inputStream);
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

    private class GSTransformer implements Transformer<InputStream, byte[]> {

        public static final String ARGS = "-dNOPAUSE -sDEVICE=png16m -dDEVICEWIDTH=${width} -dDEVICEHEIGHT=${height} -dPDFFitPage -dFirstPage=${page} " +
                "-dLastPage=${page} -sOutputFile=%stdout%  -q -_ -c quit";

        @Override
        public byte[] transform(InputStream bytes) {
            ExecuteWatchdog watchdog = new ExecuteWatchdog(TIMEOUT_PAA_GENERERING);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                CommandLine cmdLine = new CommandLine(decideGSCommand());
                cmdLine.addArguments(ARGS);
                cmdLine.setSubstitutionMap(genererThumbnailProperties());

                DefaultExecutor executor = new DefaultExecutor();
                executor.setWorkingDirectory(TMP_DIR);

                executor.setStreamHandler(new PumpStreamHandler(out, System.err, bytes));
                executor.setWatchdog(watchdog);

                executor.execute(cmdLine);

                return out.toByteArray();
            } catch (ExecuteException e) {
                if (watchdog.killedProcess()) {
                    LOGGER.info("[PDF] Timet ut mens en lagde preview.");
                    throw new RuntimeException("Prosessen brukte for lang tid", e);
                } else {
                    LOGGER.info("[PDF] Noe gikk galt i GhostScript under generering av preview. ", e);
                    throw new RuntimeException("Noe annet gikk galt: " + e, e);
                }
            } catch (IOException e) {
                LOGGER.info("[PDF] Noe gikk galt i GhostScript under generering av preview. ", e);
                throw new RuntimeException("Feil ved preview-generering", e);
            }
        }

        private Map<String, String> genererThumbnailProperties() {
            Map<String, String> substMap = new HashMap<>();
            substMap.put("width", "" + boundingBox.width);
            substMap.put("height", "" + boundingBox.height);
            substMap.put("page", "" + (side + 1));
            return substMap;
        }

    }

    private static String decideGSCommand() {
        return OS.isFamilyWindows() ? "gswin32c" : "gs";
    }

    private static boolean checkIfGhostScriptExists() {
        int exitCode = -1;
        try {
            CommandLine cmdLine = new CommandLine(decideGSCommand());
            cmdLine.addArgument("--version");
            DefaultExecutor executor = new DefaultExecutor();
            executor.setWorkingDirectory(TMP_DIR);
            exitCode = executor.execute(cmdLine);
        } catch (IOException e) {
            LOGGER.warn("[PDF] Could not find ghostscript");
            LOGGER.warn("[PDF] " + e, e);
        }
        boolean exists = (exitCode == 0);
        if (GS_EXISTS) {
            LOGGER.info("[PDF] Bruker GhostScript som rendring-motor for thumbnails");
        } else {
            LOGGER.warn("[PDF] Fant ikke GhostScript. Bruker PdfBox som rendring-motor for thumbnails (exitcode: {})", exitCode);
        }
        return exists;
    }


}
