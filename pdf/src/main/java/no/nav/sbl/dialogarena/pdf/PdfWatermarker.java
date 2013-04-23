package no.nav.sbl.dialogarena.pdf;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import no.nav.sbl.dialogarena.detect.IsPdf;
import org.apache.commons.collections15.Transformer;
import org.joda.time.DateTime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import static com.itextpdf.text.BaseColor.BLACK;
import static com.itextpdf.text.BaseColor.RED;
import static com.itextpdf.text.BaseColor.WHITE;
import static com.itextpdf.text.pdf.BaseFont.EMBEDDED;
import static com.itextpdf.text.pdf.BaseFont.HELVETICA;
import static com.itextpdf.text.pdf.BaseFont.WINANSI;
import static com.itextpdf.text.pdf.PdfContentByte.ALIGN_LEFT;
import static java.lang.Math.max;

/**
 * Vannmerker PDF
 *
 * For flersidig PDF legges vannmerket på samtlige sider.
 */
public class PdfWatermarker {

    public static final String LINE_1_HEADER = "Sendt elektronisk: ";
    public static final String LINE_2_HEADER = "Fødselsnummer: ";

    private static final int MARGIN = 5;
    private static final int PADDING_X = 5;
    private static final int PADDING_Y = 5;
    private static final int LINE_SPACING = 5;

    private static final BaseColor BACKGROUND_COLOR = WHITE;
    private static final float BACKGROUND_OPACITY = 0.9f;
    private static final BaseColor TEXT_COLOR = BLACK;
    private static final BaseColor BORDER_COLOR = RED;
    private static final float BORDER_WIDTH = 1f;
    private static final int FONT_SIZE = 6;


    public final Transformer<byte[], byte[]> forIdent(String ident) {
        return new WatermarkerForIdent(ident);
    }

    public final byte[] applyOn(byte[] bytes, String ident) {
        if (!(new IsPdf().evaluate(bytes))) {
            throw new IllegalArgumentException("Kan kun vannmerke PDF-filer.");
        }

        String linje1 = LINE_1_HEADER + formatertDato();
        String linje2 = LINE_2_HEADER + ident;

        byte[] watermarkedPdf;
        try {
            PdfReader originalReader = new PdfReader(bytes);
            byte[] rectangleStampedPdf = stampRectangleOnPdf(originalReader, linje1, linje2);
            PdfReader rectangleReader = new PdfReader(rectangleStampedPdf);
            watermarkedPdf = stampTextOnPdf(rectangleReader, linje1, linje2);
        } catch (DocumentException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return watermarkedPdf;
    }


    private byte[] stampRectangleOnPdf(PdfReader reader, String linje1, String linje2) throws IOException, DocumentException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfStamper stamper = new PdfStamper(reader, byteArrayOutputStream);

        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            float pageWidth = reader.getPageSize(i).getWidth();
            float pageHeigth = reader.getPageSize(i).getHeight();

            PdfContentByte rectangleContent = stamper.getOverContent(i);
            rectangleContent.setColorStroke(TEXT_COLOR);
            rectangleContent.setFontAndSize(createFont(), FONT_SIZE);

            float lineWidth = max(rectangleContent.getEffectiveStringWidth(linje1, false),
                    rectangleContent.getEffectiveStringWidth(linje2, false));

            PdfGState gstate = new PdfGState();
            gstate.setFillOpacity(BACKGROUND_OPACITY);
            rectangleContent.setGState(gstate);
            rectangleContent.rectangle(watermarkFrame(pageWidth, pageHeigth, lineWidth));

        }
        stamper.close();
        return byteArrayOutputStream.toByteArray();
    }

    private byte[] stampTextOnPdf(PdfReader reader, String linje1, String linje2) throws IOException, DocumentException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfStamper stamper = new PdfStamper(reader, byteArrayOutputStream);

        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            float pageWidth = reader.getPageSize(i).getWidth();
            float pageHeigth = reader.getPageSize(i).getHeight();

            PdfContentByte textContent = stamper.getOverContent(i);
            textContent.setColorStroke(TEXT_COLOR);
            textContent.setFontAndSize(createFont(), FONT_SIZE);

            float lineWidth = max(textContent.getEffectiveStringWidth(linje1, false),
                    textContent.getEffectiveStringWidth(linje2, false));

            float magicNumber = 2f; // Dette trengs for å få riktig vertikal offset
            float textX = pageWidth - MARGIN - lineWidth - PADDING_X;
            float linje1Y = pageHeigth - MARGIN - BORDER_WIDTH - PADDING_Y - FONT_SIZE / 2 - magicNumber;
            float linje2Y = linje1Y - FONT_SIZE - LINE_SPACING;

            textContent.beginText();
            textContent.showTextAligned(ALIGN_LEFT, linje1, textX, linje1Y, 0);
            textContent.showTextAligned(ALIGN_LEFT, linje2, textX, linje2Y, 0);
            textContent.endText();
        }
        stamper.close();
        return byteArrayOutputStream.toByteArray();
    }

    private String formatertDato() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.YYYY', kl. 'HH:mm:ss");
        return simpleDateFormat.format(DateTime.now().toDate());
    }

    private Rectangle watermarkFrame(float pageWidth, float pageHeigth, float lineWidth) {
        float upperRightX = pageWidth - MARGIN;
        float upperRightY = pageHeigth - MARGIN;
        float lowerLeftX = upperRightX - lineWidth - 2 * PADDING_X;
        float lowerLeftY = upperRightY - 2 * FONT_SIZE - 2 * PADDING_Y - LINE_SPACING - 2 * BORDER_WIDTH;

        Rectangle frame = new Rectangle(lowerLeftX, lowerLeftY, upperRightX, upperRightY);
        frame.setBackgroundColor(BACKGROUND_COLOR);
        frame.enableBorderSide(Rectangle.BOX);
        frame.setBorderColor(BORDER_COLOR);
        frame.setBorderWidth(BORDER_WIDTH);
        return frame;
    }

    private BaseFont createFont() throws DocumentException, IOException {
        return BaseFont.createFont(HELVETICA, WINANSI, EMBEDDED);
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
}

