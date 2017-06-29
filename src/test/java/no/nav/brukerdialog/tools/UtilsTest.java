package no.nav.brukerdialog.tools;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;


public class UtilsTest {

    @Test
    public void skalKutteVekkDomene() {
        String path = "https://app.adeo.no/veilarbportefoljeflatefs/tjenester/login";
        assertThat(Utils.getRelativePath(path)).isEqualTo("/veilarbportefoljeflatefs/tjenester/login");
    }

    @Test
    public void skalKutteVekkDomene2() {
        String path = "https://localhost:9592/veilarbportefoljeflatefs/tjenester/login";
        assertThat(Utils.getRelativePath(path)).isEqualTo("/veilarbportefoljeflatefs/tjenester/login");
    }



}