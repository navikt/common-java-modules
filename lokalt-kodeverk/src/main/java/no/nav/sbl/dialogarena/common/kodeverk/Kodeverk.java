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
     * @param skjemaId SkjemaId
     * @param nokkel   Hvilken del av kodeverket som skal hentes
     * @return Kodeverkverdi
     */
    String getKode(String skjemaId, Nokkel nokkel);

    /**
     * Behagelighetsfunksjon for å hente tittel-del av kodeverk
     * @param skjemaId SkjemaId
     * @return tittel
     */
    String getTittel(String skjemaId) ;

    /**
     * Henter ut alle nøkkel-verdier for en kode-verdi.
     * Kaster ApplicationException hvis kode ikke finnes.
     *
     * @param skjemaId SkjemaId
     * @return Map med alle kodeverkverdier.
     */
    Map<Nokkel, String> getKoder(String skjemaId);

    /**
     * Sjekker om kode-verdi er lik den magiske annet
     * @param skjemaId SkjemaId
     * @return true hvis skjemaId er ANNET, ellers false
     */
    boolean isEgendefinert(String skjemaId);

    /**
     * Nøkkelverdier som det finnes kodeverk for.
     */
    enum Nokkel {
        GOSYS_ID, TEMA, TITTEL, BESKRIVELSE, URL, URLENGLISH, VEDLEGGSID
    }
}
