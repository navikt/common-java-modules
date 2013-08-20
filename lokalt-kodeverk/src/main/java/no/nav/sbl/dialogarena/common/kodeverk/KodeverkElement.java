package no.nav.sbl.dialogarena.common.kodeverk;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.BESKRIVELSE;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.GOSYS_ID;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.SKJEMANUMMER;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.TEMA;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.TITTEL;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.URL;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.URLENGLISH;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.URLNEWNORWEGIAN;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.URLPOLISH;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.URLFRENCH;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.URLSPANISH;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.URLGERMAN;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.URLSAMISK;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.VEDLEGGSID;
class KodeverkElement {


    private final Map<Kodeverk.Nokkel, String> koder;

    KodeverkElement(Map<Kodeverk.Nokkel, String> kodeverkMap) {
        koder = new HashMap<>();
        koder.put(SKJEMANUMMER, kodeverkMap.get(SKJEMANUMMER));
        koder.put(BESKRIVELSE, kodeverkMap.get(BESKRIVELSE));
        koder.put(VEDLEGGSID, kodeverkMap.get(VEDLEGGSID));
        koder.put(GOSYS_ID, kodeverkMap.get(GOSYS_ID));
        koder.put(TEMA, kodeverkMap.get(TEMA));
        koder.put(TITTEL, kodeverkMap.get(TITTEL));
        koder.put(URL, kodeverkMap.get(URL));
        koder.put(URLENGLISH, kodeverkMap.get(URLENGLISH));
        koder.put(URLNEWNORWEGIAN, kodeverkMap.get(URLNEWNORWEGIAN));
        koder.put(URLPOLISH, kodeverkMap.get(URLPOLISH));
        koder.put(URLFRENCH, kodeverkMap.get(URLFRENCH));
        koder.put(URLSPANISH, kodeverkMap.get(URLSPANISH));
        koder.put(URLGERMAN, kodeverkMap.get(URLGERMAN));
        koder.put(URLSAMISK, kodeverkMap.get(URLSAMISK));
    }

    Map<Kodeverk.Nokkel, String> getKoderMap() {
        return unmodifiableMap(koder);
    }

}
