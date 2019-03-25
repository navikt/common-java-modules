package no.nav.apiapp.security;

import org.junit.Test;

import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static junit.framework.TestCase.fail;

public class VeilarbAbacPepClientBrukerTest {

    private static final String FNR = "fnr";
    private static final String AKTOER_ID = "aktorId";

    @Test(expected = IllegalStateException.class)
    public void testUfullstentigBruker() {
        VeilarbAbacPepClient.Bruker.ny().bygg();
    }

    @Test(expected = IllegalStateException.class)
    public void testUfullstentigBrukerBareAktoerId() {
        VeilarbAbacPepClient.Bruker.ny().medAktoerId(AKTOER_ID).bygg();
    }

    @Test(expected = IllegalStateException.class)
    public void testUfullstentigBrukerBareFnr() {
        VeilarbAbacPepClient.Bruker.ny().medFoedeselsnummer(FNR).bygg();
    }

    @Test(expected = IllegalStateException.class)
    public void testUfullstentigBrukerBareFnrOgUgyldigKonvertering() {
        VeilarbAbacPepClient.Bruker.ny().medFoedeselsnummer(FNR).medAktoerIdTilFoedselsnummerKonvertering(s->s).bygg();
    }

    @Test(expected = IllegalStateException.class)
    public void testUfullstentigBrukerBareAktoerIdOgUgyldigKonvertering() {
        VeilarbAbacPepClient.Bruker.ny().medAktoerId(AKTOER_ID).medFoedselnummerTilAktoerIdKonvertering(s->s).bygg();
    }

    @Test()
    public void testGyldigeBrukere() {
        VeilarbAbacPepClient.Bruker.ny().medFoedeselsnummer(FNR).medAktoerId(AKTOER_ID).bygg();
        VeilarbAbacPepClient.Bruker.ny().medFoedeselsnummer(FNR).medFoedselnummerTilAktoerIdKonvertering(fnr->AKTOER_ID).bygg();
        VeilarbAbacPepClient.Bruker.ny().medAktoerId(AKTOER_ID).medAktoerIdTilFoedselsnummerKonvertering(aktorId->FNR).bygg();
    }

}