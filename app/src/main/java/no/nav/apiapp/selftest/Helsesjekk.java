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
            return Ping.lyktes(metadata.getEndepunkt(), metadata.getBeskrivelse(), metadata.isKritisk());
        } catch (Exception e) {
            return Ping.feilet(metadata.getEndepunkt(), metadata.getBeskrivelse(), metadata.isKritisk(), e);
        }
    }

}
