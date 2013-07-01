package no.nav.sbl.dialogarena.common.kodeverk;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.BESKRIVELSE;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.GOSYS_ID;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.SKJEMANUMMER;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.TEMA;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.TITTEL;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.URL;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.URLENGLISH;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.VEDLEGGSID;

class KodeverkElement {


    private final Map<Nokkel, String> koder;

    KodeverkElement(Map<Nokkel, String> kodeverkMap) {
        koder = new HashMap<>();
        koder.put(SKJEMANUMMER, kodeverkMap.get(SKJEMANUMMER));
        koder.put(BESKRIVELSE, kodeverkMap.get(BESKRIVELSE));
        koder.put(VEDLEGGSID, kodeverkMap.get(VEDLEGGSID));
        koder.put(GOSYS_ID, kodeverkMap.get(GOSYS_ID));
        koder.put(TEMA, kodeverkMap.get(TEMA));
        koder.put(TITTEL, kodeverkMap.get(TITTEL));
        koder.put(URL, kodeverkMap.get(URL));
        koder.put(URLENGLISH, kodeverkMap.get(URLENGLISH));
    }

    Map<Nokkel, String> getKoderMap() {
        return unmodifiableMap(koder);
    }
}