package no.nav.sbl.dialogarena.common.tilbakemelding.service;
import no.nav.sbl.dialogarena.common.tilbakemelding.service.EpostCleaner;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;



public class EpostCleanerTest {


    @Test
    public void shouldRemoveIllegalCharacters() {

        String string1 = "<html>test\n\r</html>\n\r";
        String string1cleaned = EpostCleaner.cleanbody(string1);
        assertThat(string1cleaned, is("test"));

    }


}
