package no.nav.brukerdialog.tools;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;


public class UtilsTest {

    @Test
    public void skalKutteVekkDomene() {
        String path = "https://app.adeo.no/veilarbportefoljeflatefs/api/login";
        assertThat(Utils.getRelativePath(path)).isEqualTo("/veilarbportefoljeflatefs/api/login");
    }

    @Test
    public void skalKutteVekkDomene2() {
        String path = "https://localhost:9592/veilarbportefoljeflatefs/api/login";
        assertThat(Utils.getRelativePath(path)).isEqualTo("/veilarbportefoljeflatefs/api/login");
    }



}