package no.nav.apiapp.selftest;

import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;

public class HelsesjekkMetadata extends PingMetadata {
    public HelsesjekkMetadata(String endepunkt, String beskrivelse, boolean kritisk) {
        super(endepunkt, beskrivelse, kritisk);
    }
}
