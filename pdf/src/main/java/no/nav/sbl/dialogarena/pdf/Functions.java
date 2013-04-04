package no.nav.sbl.dialogarena.pdf;

import com.itextpdf.text.pdf.PdfReader;
import org.apache.commons.collections15.Transformer;

import java.io.IOException;

/**
 * Statiske funksjoner for operasjoner på PDFer, inkl henting av antall sider
 */

public class Functions {

    public static final Transformer<byte[], Integer> PDF_SIDEANTALL = new Transformer<byte[], Integer>() {
        @Override
        public Integer transform(byte[] pdfdata) {
            try {
                return new PdfReader(pdfdata).getNumberOfPages();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    };
}
