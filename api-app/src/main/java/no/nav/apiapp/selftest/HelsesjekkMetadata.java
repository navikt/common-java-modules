package no.nav.apiapp.selftest;

import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;

public class HelsesjekkMetadata extends PingMetadata {

    public HelsesjekkMetadata(String id, String endepunkt, String beskrivelse, boolean kritisk) {
        super(id, endepunkt, beskrivelse, kritisk);
    }

}
