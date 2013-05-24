package no.nav.sbl.dialogarena.common.kodeverk;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class DownloadedFileTest {

    private static final String JSONFILE_LOCATION_IDE = "lokalt-kodeverk/target/classes/kodeverk.json";
    private static final String JSONFILE_LOCATION_MAVEN = "target/classes/kodeverk.json";

    @Test
    public void shouldParseDownloadedFile() throws FileNotFoundException {
        FileInputStream jsonFile;
        // Prøver å åpne fil slik IDE ser den først. Hvis det ikke fungerer, forsøk slik Maven ser den.
        try {
            jsonFile = new FileInputStream(JSONFILE_LOCATION_IDE);
        } catch (FileNotFoundException ex) {
            try {
                jsonFile = new FileInputStream(JSONFILE_LOCATION_MAVEN);
            } catch (FileNotFoundException ex2) {
                throw ex2;
            }
        }
        System.out.println("Trying to parse new kodeverk");
        new JsonKodeverk(jsonFile);
        System.out.println("Kodeverk OK!");
    }
}
