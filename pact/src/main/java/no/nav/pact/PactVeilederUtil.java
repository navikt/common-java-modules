package no.nav.pact;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;

public class PactVeilederUtil {

    private static final String FASIT_ALIAS = "priveligert_veileder";

    static {
        SSLTestUtils.disableCertificateChecks();
    }

    /**
     * Bruker denne for å unngå å hardkode veileder ider i Pact-kontrakter.
     */
    public static String hentVeilederIdFraFasit() {
        return FasitUtils.getTestUser(FASIT_ALIAS, FasitUtils.getDefaultEnvironment()).getUsername();
    }

}
