package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.collections15.Transformer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Statiske funksjoner for operasjoner på PDFer, inkl henting av antall sider
 */

public class PdfFunctions {
    private static final Logger LOG = LoggerFactory.getLogger(PdfFunctions.class);

    public static final Transformer<byte[], Integer> PDF_SIDEANTALL = new Transformer<byte[], Integer>() {
        @Override
        public Integer transform(byte[] pdfdata) {
            PDDocument document = null;
            try {
                document = PDDocument.load(new ByteArrayInputStream(pdfdata));
                return document.getNumberOfPages();
            } catch (IOException e) {
                LOG.warn("Could not get number of pages from pdf document: " + e, e);
                return 0;
            } finally {
                if (document != null) {
                    try {
                        document.close();
                    } catch (IOException ignore) {
                    }
                }

            }
        }
    };
}
