package no.nav.apiapp.selftest;

import no.nav.sbl.dialogarena.types.Pingable;

public interface Helsesjekk extends Pingable {
    void helsesjekk() throws Throwable;
    HelsesjekkMetadata getMetadata();

    @Override
    default Ping ping() {
        HelsesjekkMetadata metadata = getMetadata();

        try {
            helsesjekk();
            return Ping.lyktes(metadata);
        } catch (Throwable e) {
            return Ping.feilet(metadata, e);
        }
    }

}
