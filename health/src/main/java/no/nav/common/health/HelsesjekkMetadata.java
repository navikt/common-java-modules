package no.nav.common.health;


import no.nav.common.health.domain.Pingable;

public class HelsesjekkMetadata extends Pingable.Ping.PingMetadata {

    public HelsesjekkMetadata(String id, String endepunkt, String beskrivelse, boolean kritisk) {
        super(id, endepunkt, beskrivelse, kritisk);
    }

}
