package no.nav.sbl.dialogarena.common.kodeverk;

import no.nav.modig.core.exception.ApplicationException;

import java.util.HashMap;
import java.util.Map;


/**
 * Baseklasse for implemntasjon av kodeverk-interface
 */
abstract class BaseKodeverk implements Kodeverk {
    protected final Map<String, KodeverkElement> db = new HashMap<>();

    @Override
    public boolean isEgendefinert(String skjemaId) {
        return ANNET.equals(skjemaId);
    }

    @Override
    public String getTittel(String skjemaId) {
        return getKode(skjemaId, Nokkel.TITTEL);
    }

    @Override
    public String getKode(String skjemaId, Nokkel nokkel) {
        if (db.containsKey(skjemaId)) {
            KodeverkElement kodeverkElement = db.get(skjemaId);
            return kodeverkElement.getKoderMap().get(nokkel);
        }
        handleError(skjemaId);
        return null;
    }

    @Override
    public Map<Nokkel, String> getKoder(String skjemaId) {
        if (db.containsKey(skjemaId)) {
            return db.get(skjemaId).getKoderMap();
        }
        handleError(skjemaId);
        return null;
    }

    private void handleError(String skjemaId) {
        throw new ApplicationException("Fant ikke kodeverk : " + skjemaId);
    }
}

