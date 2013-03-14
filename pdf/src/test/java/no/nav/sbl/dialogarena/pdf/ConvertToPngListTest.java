package no.nav.sbl.dialogarena.pdf;

import no.nav.sbl.dialogarena.detect.IsJpg;
import no.nav.sbl.dialogarena.detect.IsPdf;
import no.nav.sbl.dialogarena.detect.IsPng;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static no.nav.sbl.dialogarena.pdf.PdfTestUtils.getBytesFromFile;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class ConvertToPngListTest {

    @Test
    public void convertPdfToPng() throws IOException {
        byte[] pdf = getBytesFromFile("/PdfToImageFiles/pdf-file.pdf");
        assertThat(pdf, match(new IsPdf()));

        List<byte[]> pngs = new ConvertToPngList().transform(pdf);
        assertThat(pngs, hasSize(6));

        for (byte [] png : pngs) {
           assertThat(png, match(new IsPng()));
        }

        try {
            String myDirectoryPath = ConvertToPngListTest.class.getResource("/PdfToImageFiles").getPath() + "/multiple-pdf-files-converted";
            File myDirectory = new File(myDirectoryPath);
            FileUtils.deleteDirectory(myDirectory);
            myDirectory.mkdir();
            for (int i = 0; i < pngs.size(); i++) {
                OutputStream out = new BufferedOutputStream(new FileOutputStream(myDirectoryPath + "/page" + Integer.toString(i + 1) + ".png"));
                out.write(pngs.get(i));
                out.close();
            }
        } catch  (IOException e) {
            throw new RuntimeException(e);
        }
    }
//        TODO: Lag test som sjekker at bildene ligger i riktig rekkefølge etter at de er convertet fra en pdf-fil med flere sider
//    @Test
//    public void imagesInRightOrder() throws IOException {
//        String myDirectory = ConvertToPngListTest.class.getResource("/PdfToImageFiles").getPath();
//        String pdfFile =  "/PdfToImageFiles/pdf-file.pdf";
//        String tempPdfFile = pdfFile.substring(0,pdfFile.indexOf(".pdf")) + "-temp.pdf";
//        String temp = myDirectory +
//                tempPdfFile.substring(tempPdfFile.indexOf("pdf-file-temp.pdf"), tempPdfFile.indexOf(".pdf"));
//
//        PdfReader reader = new PdfReader(pdfFile);
//        List<byte[]> bytesMultiplePdfPages = new ConvertToPngList().transform(getBytesFromFile(pdfFile));
//        assertThat(bytesMultiplePdfPages.size(), is(reader.getNumberOfPages()));
//        for (int i = 0; i < bytesMultiplePdfPages.size(); i++) {
//            Document document = new Document(reader.getPageSizeWithRotation(1));
//            PdfCopy writer = null;
//            try {
//                writer = new PdfCopy(document, new FileOutputStream(temp));
//            } catch (DocumentException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//            document.open();
//            PdfImportedPage page = writer.getImportedPage(reader,i+1);
//            try {
//                writer.addPage(page);
//            } catch (BadPdfFormatException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//            document.close();
//            writer.close();
//
//            byte[] bytesSinglePdfPage = new ConvertToPng().transform(getBytesFromFile(tempPdfFile));
//            assertThat(bytesMultiplePdfPages.get(i), Matchers.is(bytesSinglePdfPage));
//        }
//
//    }

    @Test
    public void convertJpgToPng() throws IOException {
        byte[] jpg = getBytesFromFile("/PdfToImageFiles/jpeg-file.jpeg");

        assertThat(jpg, match(new IsJpg()));
        List<byte[]> png = new ConvertToPngList().transform(jpg);
        assertThat(png.get(0), match(new IsPng()));
    }

    @Test
    public void dontChangePng() throws IOException {
        byte[] png = getBytesFromFile("/PdfToImageFiles/png-file.png");

        assertThat(png, match(new IsPng()));
        List<byte[]> newPng = new ConvertToPngList().transform(png);
        assertThat(newPng.get(0), match(new IsPng()));
        assertThat(png, is(newPng.get(0)));


    }
}
