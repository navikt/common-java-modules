package no.nav.pact;

import com.google.gson.Gson;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

public class PactVeilederUtil {

    private static final String TESTUSER_API = "https://testbrukere.nais.preprod.local/testbrukere/api/miljo/";
    private static final String VEILEDER_TYPE = "veileder";

    static {
        SSLTestUtils.disableCertificateChecks();
    }

    public static List<Veileder> hentVeiledereFraFasit() {
        try {
            HttpResponse httpResponse = Request.Get(TESTUSER_API + FasitUtils.getDefaultEnvironment()).execute().returnResponse();
            Veiledere veiledere = new Gson().fromJson(new InputStreamReader(httpResponse.getEntity().getContent()), Veiledere.class);
            return veiledere.getVeiledere();
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke hente veileder fra fasit.", e);
        }
    }

    public static Veileder hentVeilederFraFasit() {
        Optional<Veileder> veileder = hentVeiledereFraFasit()
                .stream()
                .filter(v -> v.getType().equalsIgnoreCase(VEILEDER_TYPE))
                .findFirst();
        return veileder.orElseThrow(() -> new IllegalStateException("Ingen veiledere ble funnet i Fasit."));
    }

    public static String hentVeilederIdFraFasit() {
        Veileder veileder = hentVeilederFraFasit();
        return veileder.getTestuser().getUsername();
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
