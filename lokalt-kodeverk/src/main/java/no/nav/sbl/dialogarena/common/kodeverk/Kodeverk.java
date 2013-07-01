package no.nav.sbl.dialogarena.common.kodeverk;

import java.util.Map;

/**
 * Tilbyr uthenting av lokalt kodeverk.
 */
public interface Kodeverk {
    String ANNET = "N6";
    /* ?? */

    /**
     * Henter verdi basert på kode-verdi og nøkkel.
     * Kaster ApplicationException hvis kode ikke finnes.
     *
     * @param vedleggsIdOrSkjemaId SkjemaId
     * @param nokkel   Hvilken del av kodeverket som skal hentes
     * @return Kodeverkverdi
     */
    String getKode(String vedleggsIdOrSkjemaId, Nokkel nokkel);

    /**
     * Behagelighetsfunksjon for å hente tittel-del av kodeverk
     * @param vedleggsIdOrSkjemaId SkjemaId
     * @return tittel
     */
    String getTittel(String vedleggsIdOrSkjemaId) ;

    /**
     * Henter ut alle nøkkel-verdier for en kode-verdi.
     * Kaster ApplicationException hvis kode ikke finnes.
     *
     * @param vedleggsIdOrskjemaId SkjemaId
     * @return Map med alle kodeverkverdier.
     */
    Map<Nokkel, String> getKoder(String vedleggsIdOrskjemaId);

    /**
     * Sjekker om kode-verdi er lik den magiske annet
     * @param vedleggsIdOrskjemaId SkjemaId
     * @return true hvis skjemaId er ANNET, ellers false
     */
    boolean isEgendefinert(String vedleggsIdOrskjemaId);

    /**
     * Nøkkelverdier som det finnes kodeverk for.
     */
    enum Nokkel {
        SKJEMANUMMER, GOSYS_ID, TEMA, TITTEL, BESKRIVELSE, URL, URLENGLISH, URLNEWNORWEGIAN, URLPOLISH, URLFRENCH, URLSPANISH, URLGERMAN, VEDLEGGSID
    }
}
