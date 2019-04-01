package no.nav.apiapp.security.veilarbabac;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class BrukerTest {

    private static final String FNR = "fnr";
    private static final String AKTOER_ID = "aktorId";

    @Test
    public void testBeggeSatt() {
        Bruker bruker = Bruker.fraFnr(FNR).medAktoerId(AKTOER_ID);

        assertEquals(FNR,bruker.getFoedselsnummer());
        assertEquals(AKTOER_ID,bruker.getAktoerId());
     }

    @Test
    public void testFnrOgMapperSatt() {
        Bruker bruker = Bruker.fraFnr(FNR)
                .medAktoerIdSupplier(()->AKTOER_ID);

        assertEquals(FNR,bruker.getFoedselsnummer());
        assertEquals(AKTOER_ID,bruker.getAktoerId());
    }

    @Test
    public void testAktoerIdOgMapperSatt() {
        Bruker bruker = Bruker.fraAktoerId(AKTOER_ID)
                .medFoedselnummerSupplier(()->FNR);

        assertEquals(FNR,bruker.getFoedselsnummer());
        assertEquals(AKTOER_ID,bruker.getAktoerId());
    }

    @Test
    public void testBrukerMedSammeFnrOgAktoerIdErLik() {
        Bruker bruker1 = Bruker.fraFnr(FNR).medAktoerId(AKTOER_ID);
        Bruker bruker2 = Bruker.fraFnr(FNR).medAktoerId(AKTOER_ID);

        assertEquals(bruker1.hashCode(),bruker2.hashCode());
        assertEquals(bruker1,bruker2);
    }

    @Test
    public void testBrukerMedSammeFnrErLikSelvOmFnrTilAktoerIdMappingErForskjellig() {
        Bruker bruker1 = Bruker.fraFnr(FNR).medAktoerIdSupplier(()->"A");
        Bruker bruker2 = Bruker.fraFnr(FNR).medAktoerIdSupplier(()->"B");

        assertEquals(bruker1.hashCode(),bruker2.hashCode());
        assertEquals(bruker1,bruker2);
    }

    @Test
    public void testBrukerMedSammeAktoerIdErLikSelvOmAktoerIdTilFnrMappingErForskjellig() {
        Bruker bruker1 = Bruker.fraAktoerId(AKTOER_ID).medFoedselnummerSupplier(()->"A");
        Bruker bruker2 = Bruker.fraAktoerId(AKTOER_ID).medFoedselnummerSupplier(()->"B");

        assertEquals(bruker1.hashCode(),bruker2.hashCode());
        assertEquals(bruker1,bruker2);
    }

    @Test
    public void testBrukerMedSammeFnrOgForskjelligAktoerIdErUlik() {
        Bruker bruker1 = Bruker.fraFnr(FNR).medAktoerId(AKTOER_ID);
        Bruker bruker2 = Bruker.fraFnr(FNR).medAktoerId(AKTOER_ID+"A");

        assertNotEquals(bruker1.hashCode(),bruker2.hashCode());
        assertNotEquals(bruker1,bruker2);
    }

    @Test
    public void testBrukerMedSammeAktoerIdOgForskjelligFnrErUlik() {
        Bruker bruker1 = Bruker.fraAktoerId(AKTOER_ID).medFoedselsnummer(FNR);
        Bruker bruker2 = Bruker.fraAktoerId(AKTOER_ID).medFoedselsnummer(FNR+"A");

        assertNotEquals(bruker1.hashCode(),bruker2.hashCode());
        assertNotEquals(bruker1,bruker2);
    }

}