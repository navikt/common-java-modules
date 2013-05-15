package no.nav.sbl.dialogarena.common.kodeverk;

import java.util.Map;


public interface Kodeverk {
    String getKode(String skjemaId, Nokkel nokkel);

    Map<Nokkel, String> getKoder(String skjemaId);

    enum Nokkel {GOSYS_ID, TEMA, TITTEL, BESKRIVELSE, URL}
}
