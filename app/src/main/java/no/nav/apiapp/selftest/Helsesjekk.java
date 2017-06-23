package no.nav.apiapp.selftest;

import no.nav.sbl.dialogarena.types.Pingable;

public interface Helsesjekk extends Pingable {
    void helsesjekk();
    HelsesjekkMetadata getMetadata();

    @Override
    default Ping ping() {
        HelsesjekkMetadata metadata = getMetadata();

        try {
            helsesjekk();
            return Ping.lyktes(metadata);
        } catch (Exception e) {
            return Ping.feilet(metadata, e);
        }
    }

}
