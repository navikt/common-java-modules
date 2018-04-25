package no.nav.pact;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;
import no.nav.sbl.rest.RestUtils;

import java.util.List;

public class PactVeilederUtil {

    private static final String TESTUSER_API = "https://testbrukere.nais.preprod.local/testbrukere/api/miljo/";
    private static final String VEILEDER_TYPE = "veileder";

    static {
        SSLTestUtils.disableCertificateChecks();
    }

    public static List<Veileder> hentVeiledereFraFasit() {
        Veiledere veiledere = RestUtils.withClient(client ->
                client.target(TESTUSER_API + FasitUtils.getDefaultEnvironment()).request().get(Veiledere.class));
        return veiledere.getVeiledere();
    }

    public static Veileder hentVeilederFraFasit() {
        return hentVeiledereFraFasit()
                .stream()
                .filter(v -> v.getType().equalsIgnoreCase(VEILEDER_TYPE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Ingen veiledere ble funnet i Fasit."));
    }

    public static String hentVeilederIdFraFasit() {
        return hentVeilederFraFasit().getTestuser().getUsername();
    }

    @Data
    @Accessors
    public static final class Veiledere {
        List<Veileder> veiledere;
    }

    @Data
    @Accessors
    public static final class Veileder {
        String fasitAlias;
        String type;
        Testuser testuser;

        @Override
        public String toString() {
            return "Veileder{" +
                    "fasitAlias='" + fasitAlias + '\'' +
                    ", type='" + type + '\'' +
                    ", testuser=" + testuser +
                    '}';
        }
    }

    @Data
    public static final class Testuser {
        String username;
        String password;

        @Override
        public String toString() {
            return "Testuser{" +
                    "username='" + username + '\'' +
                    '}';
        }
    }

}
