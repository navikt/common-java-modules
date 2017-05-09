package no.nav.apiapp.selftest;

import no.nav.sbl.dialogarena.types.Pingable;

public interface Helsesjekk extends Pingable {

    void helsesjekk();

    @Override
    default Ping ping() {
        String komponent = getClass().getSimpleName();
        try {
            helsesjekk();
            return Ping.lyktes(komponent);
        } catch (Exception e) {
            return Ping.feilet(komponent, e);
        }
    }

}
