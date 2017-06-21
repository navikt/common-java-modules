package no.nav.fo.apiapp.selftest;

import no.nav.sbl.dialogarena.types.Pingable;

import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;

public class PingableEksempel implements Pingable {

    private static final String EKSEMPEL = "EKSEMPEL_V1";
    private static final String EKSEMPEL_BESKRIVELSE = "En beskrivelse av endepunktet.";

    private boolean ok = true;

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    @Override
    public Ping ping() {
        return ok ? lyktes(EKSEMPEL, EKSEMPEL_BESKRIVELSE) : feilet(EKSEMPEL, EKSEMPEL_BESKRIVELSE, true, new RuntimeException());
    }

}
