package no.nav.sbl.dialogarena.common.kodeverk;

import org.junit.Test;

import java.io.FileNotFoundException;

public class DownloadedFileTest {

    @Test
    @SuppressWarnings("PMD.SystemPrintln")
    public void shouldParseDownloadedFile() throws FileNotFoundException {
        System.out.println("Trying to parse new kodeverk");
        new JsonKodeverk(DownloadedFileTest.class.getResourceAsStream("/kodeverk.json"));
        System.out.println("Kodeverk OK!");
    }
}
