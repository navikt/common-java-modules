package no.nav.sbl.dialogarena.common.kodeverk;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class DownloadedFileTest {

    @Test
    public void shouldParseDownloadedFile() throws FileNotFoundException {
        FileInputStream jsonFile;
        System.out.println("Trying to parse new kodeverk");
        new JsonKodeverk(DownloadedFileTest.class.getResourceAsStream("/kodeverk.json"));
        System.out.println("Kodeverk OK!");
    }
}
