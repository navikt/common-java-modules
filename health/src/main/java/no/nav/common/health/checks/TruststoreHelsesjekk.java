package no.nav.common.health.checks;

import no.nav.common.health.Helsesjekk;
import no.nav.common.health.HelsesjekkMetadata;

import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;

public class TruststoreHelsesjekk implements Helsesjekk {

    public static final String TRUSTSTORE = "javax.net.ssl.trustStore";
    public static final String TRUSTSTOREPASSWORD = "javax.net.ssl.trustStorePassword";
    public static final String NAV_TRUSTSTORE_PATH = "NAV_TRUSTSTORE_PATH";
    public static final String NAV_TRUSTSTORE_PASSWORD = "NAV_TRUSTSTORE_PASSWORD";

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
