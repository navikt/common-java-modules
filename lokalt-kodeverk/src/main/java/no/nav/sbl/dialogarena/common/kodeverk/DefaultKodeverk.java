package no.nav.sbl.dialogarena.common.kodeverk;

import no.nav.modig.core.exception.ApplicationException;

import java.util.HashMap;
import java.util.Map;


public class DefaultKodeverk implements Kodeverk {
    private Map<String, KodeverkElement> db = new HashMap<>();

    @Override
    public String getKode(String skjemaId, Nokkel nokkel) {
        if (db.containsKey(skjemaId)) {
            KodeverkElement kodeverkElement = db.get(skjemaId);
            switch (nokkel) {
                case BESKRIVELSE:
                    return kodeverkElement.getBeskrivelseNoekkel();
                case GOSYS_ID:
                    return kodeverkElement.getGosysId();
                case TEMA:
                    return kodeverkElement.getTema();
                case TITTEL:
                    return kodeverkElement.getTittel();
                case URL:
                    return kodeverkElement.getUrl();
            }
        }
        throw new ApplicationException("Fant ikke kodeverk : " + skjemaId);
    }

    @Override
    public Map<Nokkel, String> getKoder(String skjemaId) {
        if (db.containsKey(skjemaId)) {
            return db.get(skjemaId).getKoderMap();
        }
        throw new ApplicationException("Fant ikke kodeverk : " + skjemaId);
    }
}
