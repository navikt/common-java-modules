package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.collections15.Transformer;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayInputStream;
import java.io.IOException;


public class Functions {

    public static final Transformer<byte[], Integer> PDF_SIDEANTALL = new Transformer<byte[], Integer>() {
        @Override
        public Integer transform(byte[] pdfdata) {
            try {
                return PDDocument.load(new ByteArrayInputStream(pdfdata)).getNumberOfPages();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

    };
}
