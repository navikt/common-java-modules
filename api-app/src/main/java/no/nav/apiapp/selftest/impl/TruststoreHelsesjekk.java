package no.nav.apiapp.selftest.impl;

import no.nav.common.health.Helsesjekk;
import no.nav.common.health.HelsesjekkMetadata;

import static no.nav.apiapp.ApiApp.NAV_TRUSTSTORE_PATH;
import static no.nav.apiapp.ApiApp.TRUSTSTORE;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;

public class TruststoreHelsesjekk implements Helsesjekk {

    @Override
    public void helsesjekk() throws Throwable {
        getOptionalProperty(TRUSTSTORE).orElseThrow(IllegalStateException::new);
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "truststore",
                getOptionalProperty(NAV_TRUSTSTORE_PATH).orElse("N/A"),
                "Sjekker at truststore er satt",
                true
        );
    }
}
