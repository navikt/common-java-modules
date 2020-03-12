package no.nav.fo.apiapp.selftest;

import no.nav.common.health.domain.Pingable;
import no.nav.common.health.domain.Pingable.Ping.PingMetadata;

import static no.nav.common.health.domain.Pingable.Ping.feilet;
import static no.nav.common.health.domain.Pingable.Ping.lyktes;

public class PingableEksempel implements Pingable {

    private static final String EKSEMPEL_ID = "eksempel";
    private static final String EKSEMPEL_ENDEOUNKT = "EKSEMPEL_V1";
    private static final String EKSEMPEL_BESKRIVELSE = "En beskrivelse av endepunktet.";

    private boolean ok = true;

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    @Override
    public Ping ping() {
        PingMetadata metadata = new PingMetadata(EKSEMPEL_ID, EKSEMPEL_ENDEOUNKT, EKSEMPEL_BESKRIVELSE, true);
        return ok ? lyktes(metadata) : feilet(metadata, new RuntimeException());
    }

}
