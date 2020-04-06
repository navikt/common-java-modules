package no.nav.common.health;


import no.nav.common.health.selftest.Pingable;

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
