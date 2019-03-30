package no.nav.apiapp.security.veilarbabac;

import org.junit.Test;

public class BrukerTest {

    private static final String FNR = "fnr";
    private static final String AKTOER_ID = "aktorId";

    @Test(expected = IllegalStateException.class)
    public void testUfullstentigBruker() {
        Bruker.ny().bygg();
    }

    @Test(expected = IllegalStateException.class)
    public void testUfullstentigBrukerBareAktoerId() {
        Bruker.ny().medAktoerId(AKTOER_ID).bygg();
    }

    @Test(expected = IllegalStateException.class)
    public void testUfullstentigBrukerBareFnr() {
        Bruker.ny().medFoedeselsnummer(FNR).bygg();
    }

    @Test(expected = IllegalStateException.class)
    public void testUfullstentigBrukerBareFnrOgUgyldigKonvertering() {
        Bruker.ny().medFoedeselsnummer(FNR).medAktoerIdTilFoedselsnummerKonvertering(s->s).bygg();
    }

    @Test(expected = IllegalStateException.class)
    public void testUfullstentigBrukerBareAktoerIdOgUgyldigKonvertering() {
        Bruker.ny().medAktoerId(AKTOER_ID).medFoedselnummerTilAktoerIdKonvertering(s->s).bygg();
    }

    @Test()
    public void testGyldigeBrukere() {
        Bruker.ny().medFoedeselsnummer(FNR).medAktoerId(AKTOER_ID).bygg();
        Bruker.ny().medFoedeselsnummer(FNR).medFoedselnummerTilAktoerIdKonvertering(fnr->AKTOER_ID).bygg();
        Bruker.ny().medAktoerId(AKTOER_ID).medAktoerIdTilFoedselsnummerKonvertering(aktorId->FNR).bygg();
    }

}