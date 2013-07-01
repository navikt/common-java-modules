package no.nav.sbl.dialogarena.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;

import java.io.IOException;


public final class PdAutoCloseable {

    public static PDPageAutoCloseable autoClose(PDPageContentStream contentStream) {
        return new PDPageAutoCloseable(contentStream);
    }

    public static PDDocumentAutoCloseable autoClose(PDDocument document) {
        return new PDDocumentAutoCloseable(document);
    }


    public static final class PDPageAutoCloseable implements AutoCloseable {

        private PDPageContentStream contentStream;

        public PDPageAutoCloseable(PDPageContentStream contentStream) {
            this.contentStream = contentStream;
        }

        @Override
        public void close() throws IOException {
            contentStream.close();
        }

        public PDPageContentStream getContentStream() {
            return contentStream;
        }
    }


    public static final class PDDocumentAutoCloseable implements AutoCloseable {

        private PDDocument document;

        public PDDocumentAutoCloseable(PDDocument document) {
            this.document = document;
        }

        @Override
        public void close() throws IOException {
            document.close();
        }

        public PDDocument getDocument() {
            return document;
        }
    }


    private PdAutoCloseable() {
    }
}
