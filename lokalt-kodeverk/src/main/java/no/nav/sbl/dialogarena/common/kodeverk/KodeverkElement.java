package no.nav.sbl.dialogarena.common.kodeverk;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.BESKRIVELSE;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.GOSYS_ID;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.TEMA;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.TITTEL;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.URL;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.URLENGLISH;
import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel.VEDLEGGSID;
class KodeverkElement {


    private final Map<Kodeverk.Nokkel, String> koder;

    KodeverkElement(String gosysId, String vedleggsId, String tema, String beskrivelse, String tittel, String url, String urlEnglish) {
        koder = new HashMap<>();
        koder.put(BESKRIVELSE, beskrivelse);
        koder.put(VEDLEGGSID, vedleggsId);
        koder.put(GOSYS_ID, gosysId);
        koder.put(TEMA, tema);
        koder.put(TITTEL, tittel);
        koder.put(URL, url);
        koder.put(URLENGLISH, urlEnglish);
    }

    Map<Kodeverk.Nokkel, String> getKoderMap() {
        return unmodifiableMap(koder);
    }

}
