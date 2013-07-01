package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.pdf.PdAutoCloseable.PDPageAutoCloseable;
import org.apache.commons.collections15.Transformer;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDExtendedGraphicsState;
import org.joda.time.DateTime;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Math.max;
import static no.nav.sbl.dialogarena.pdf.PdAutoCloseable.autoClose;

/**
 * Vannmerker PDF
 * <p/>
 * For flersidig PDF legges vannmerket på samtlige sider.
 */
public class PdfWatermarker {

    public static final String LINE_1_HEADER = "Sendt elektronisk: ";
    public static final String LINE_2_HEADER = "Fødselsnummer: ";

    private static final int MARGIN = 5;
    private static final int PADDING_X = 5;
    private static final int PADDING_Y = 5;
    private static final int LINE_SPACING = 5;
    private static final int LINJE1_START_Y = 13;
    private static final int LINJE2_START_Y = 5;
    private static final float BACKGROUND_OPACITY = 0.8f;
    private static final float DEFAULT_BACKGROUND_OPACITY = 1.0f;
    private static final float BORDER_WIDTH = 1f;
    private static final float HEIGHT = 22f;
    private static final PDFont FONT = PDType1Font.HELVETICA;
    private static final int FONT_SIZE = 6;


    public final Transformer<byte[], byte[]> forIdent(String ident) {
        return new WatermarkerForIdent(ident);
    }

    public final byte[] applyOn(byte[] bytes, String ident) {

        PDDocument pdfDocument = null;
        String linje1 = LINE_1_HEADER + formatertDato();
        String linje2 = LINE_2_HEADER + ident;

        if (!(new IsPdf().evaluate(bytes))) {
            throw new IllegalArgumentException("Kan kun vannmerke PDF-filer.");
        }

        try {
            InputStream inputStream = new ByteArrayInputStream(bytes);
            pdfDocument = PDDocument.load(inputStream);

            stampRectangleOnPdf(pdfDocument, linje1, linje2);
            stampTextOnPdf(pdfDocument, linje1, linje2);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            pdfDocument.save(output);
            return output.toByteArray();
        } catch (COSVisitorException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            try {
                if (pdfDocument != null) {
                    pdfDocument.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    private class WatermarkerForIdent implements Transformer<byte[], byte[]> {
        private final String ident;

        public WatermarkerForIdent(String ident) {
            this.ident = ident;
        }

        @Override
        public byte[] transform(byte[] bytes) {
            return PdfWatermarker.this.applyOn(bytes, ident);
        }
    }

    private void stampRectangleOnPdf(PDDocument pdfDocument, String linje1, String linje2) throws IOException {
        @SuppressWarnings("unchecked")
        List<PDPage> allPages = pdfDocument.getDocumentCatalog().getAllPages();
        for (PDPage page : allPages) {

            float pageHeight = page.getMediaBox().getHeight();
            float pageWidth = page.getMediaBox().getWidth();
            float lineWidth = max(findLineWidthForTextWithFontSize(linje1, FONT, FONT_SIZE),
                    findLineWidthForTextWithFontSize(linje2, FONT, FONT_SIZE)) + PADDING_X + PADDING_Y;

            float upperRightX = pageWidth - MARGIN;
            float upperRightY = pageHeight - MARGIN;
            float lowerLeftX = upperRightX - lineWidth - 2 * PADDING_X;
            float lowerLeftY = upperRightY - 2 * FONT_SIZE - 2 * PADDING_Y - LINE_SPACING - 2 * BORDER_WIDTH;

            String key = "MyGraphicsState1";
            setOpacity(page.findResources(), BACKGROUND_OPACITY, key);
            try (PDPageAutoCloseable pdPage = autoClose(new PDPageContentStream(pdfDocument, page, true, true, true))) {
                pdPage.getContentStream().appendRawCommands("/" + key + " gs\n");
                pdPage.getContentStream().setNonStrokingColor(Color.white);
                pdPage.getContentStream().fillRect(lowerLeftX, lowerLeftY, lineWidth, HEIGHT);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    private void stampTextOnPdf(PDDocument pdfDocument, String linje1, String linje2) throws IOException {
        @SuppressWarnings("unchecked")
        List<PDPage> allPages = pdfDocument.getDocumentCatalog().getAllPages();
        for (PDPage page : allPages) {

            float pageHeight = page.getMediaBox().getHeight();
            float pageWidth = page.getMediaBox().getWidth();
            float lineWidth = max(findLineWidthForTextWithFontSize(linje1, FONT, FONT_SIZE),
                    findLineWidthForTextWithFontSize(linje2, FONT, FONT_SIZE)) + PADDING_X + PADDING_Y;

            float upperRightX = pageWidth - MARGIN;
            float upperRightY = pageHeight - MARGIN;
            float lowerLeftX = upperRightX - lineWidth - 2 * PADDING_X;
            float lowerLeftY = upperRightY - 2 * FONT_SIZE - 2 * PADDING_Y - LINE_SPACING - 2 * BORDER_WIDTH;

            String key = "MyGraphicsState2";
            setOpacity(page.findResources(), DEFAULT_BACKGROUND_OPACITY, key);

            try (PDPageAutoCloseable pdPage = autoClose(new PDPageContentStream(pdfDocument, page, true, true, true))) {
                pdPage.getContentStream().appendRawCommands("/" + key + " gs\n");
                pdPage.getContentStream().setFont(FONT, FONT_SIZE);
                pdPage.getContentStream().setNonStrokingColor(Color.black);

                pdPage.getContentStream().beginText();
                pdPage.getContentStream().moveTextPositionByAmount(lowerLeftX + MARGIN, lowerLeftY + LINJE1_START_Y);
                pdPage.getContentStream().drawString(linje1);
                pdPage.getContentStream().endText();

                pdPage.getContentStream().beginText();
                pdPage.getContentStream().moveTextPositionByAmount(lowerLeftX + MARGIN, lowerLeftY + LINJE2_START_Y);
                pdPage.getContentStream().drawString(linje2);
                pdPage.getContentStream().endText();

                pdPage.getContentStream().setStrokingColor(Color.red);
                pdPage.getContentStream().setLineWidth(BORDER_WIDTH);
                pdPage.getContentStream().drawLine(lowerLeftX, lowerLeftY, lowerLeftX, lowerLeftY + HEIGHT);
                pdPage.getContentStream().drawLine(lowerLeftX, lowerLeftY + HEIGHT, lowerLeftX + lineWidth, lowerLeftY + HEIGHT);
                pdPage.getContentStream().drawLine(lowerLeftX + lineWidth, lowerLeftY + HEIGHT, lowerLeftX + lineWidth, lowerLeftY);
                pdPage.getContentStream().drawLine(lowerLeftX + lineWidth, lowerLeftY, lowerLeftX, lowerLeftY);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }

        }
    }

    private void setOpacity(PDResources resources, float opacity, String key) {
        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
        graphicsState.setNonStrokingAlphaConstant(opacity);
        Map<String, PDExtendedGraphicsState> graphicsStateDictionary = resources.getGraphicsStates();
        if (graphicsStateDictionary == null) {
            graphicsStateDictionary = new TreeMap<>();
        }
        graphicsStateDictionary.put(key, graphicsState);
        resources.setGraphicsStates(graphicsStateDictionary);
    }

    private String formatertDato() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.YYYY', kl. 'HH:mm:ss");
        return simpleDateFormat.format(DateTime.now().toDate());
    }

    private float findLineWidthForTextWithFontSize(String text, PDFont font, int fontSize) throws IOException {
        return font.getStringWidth(text) / 1000 * fontSize;
    }

}

