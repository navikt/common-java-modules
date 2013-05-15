package no.nav.sbl.dialogarena.common.kodeverk;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KodeverkElement {

    private String skjemaNummer;
    private Map<Kodeverk.Nokkel, String> koder;

    public KodeverkElement(String skjemaNummer, String gosysId, String tema, String beskrivelseNoekkel, String tittel, String url) {
        this.skjemaNummer = skjemaNummer;
        koder = new HashMap<>();
        koder.put(Kodeverk.Nokkel.BESKRIVELSE, beskrivelseNoekkel);
        koder.put(Kodeverk.Nokkel.GOSYS_ID, gosysId);
        koder.put(Kodeverk.Nokkel.TEMA, tema);
        koder.put(Kodeverk.Nokkel.TITTEL, tittel);
        koder.put(Kodeverk.Nokkel.URL, url);
    }

    public Map<Kodeverk.Nokkel, String> getKoderMap() {
        return Collections.unmodifiableMap(koder);
    }

//    public String getSkjemaNummer() {
//        return skjemaNummer;
//    }

    public String getGosysId() {
        return koder.get(Kodeverk.Nokkel.GOSYS_ID);
    }

    public String getTema() {
        return koder.get(Kodeverk.Nokkel.TEMA);
    }

    public String getBeskrivelseNoekkel() {
        return koder.get(Kodeverk.Nokkel.BESKRIVELSE);
    }

    public String getTittel() {
        return koder.get(Kodeverk.Nokkel.TITTEL);
    }

    public String getUrl() {
        return koder.get(Kodeverk.Nokkel.URL);
    }

}
