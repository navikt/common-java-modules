package no.nav.sbl.dialogarena.common.kodeverk;

import java.util.Map;

/**
 *  Tilbyr uthenting av lokalt kodeverk.
 */
public interface Kodeverk {
    /**
     * Henter verdi basert på kode-verdi og nøkkel.
     * Kaster ApplicationException hvis kode ikke finnes.
     * @param skjemaId SkjemaId
     * @param nokkel Hvilken del av kodeverket som skal hentes
     * @return Kodeverkverdi
     */
    String getKode(String skjemaId, Nokkel nokkel);

    /**
     * Henter ut alle nøkkel-verdier for en kode-verdi.
     * Kaster ApplicationException hvis kode ikke finnes.
     * @param skjemaId SkjemaId
     * @return Map med alle kodeverkverdier.
     */
    Map<Nokkel, String> getKoder(String skjemaId);

    /**
     * Nøkkelverider som det finnes kodeverk for.
     */
    enum Nokkel {GOSYS_ID, TEMA, TITTEL, BESKRIVELSE, URL}
}
