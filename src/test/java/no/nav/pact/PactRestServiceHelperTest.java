package no.nav.pact;

import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

@Ignore
public class PactRestServiceHelperTest {

    @Before
    public void setUp() {
        SSLTestUtils.disableCertificateChecks();
    }

    @Test
    public void isNais_Alive() {
        assertTrue(
                new PactRestServiceHelper("https://pact-broker.nais.preprod.local")
                .withBasicAuth(System.getenv("PACT_USERNAME"), System.getenv("PACT_PASSWORD"))
                .isAlive());
    }

    @Test
    public void isCloud_Alive() {
        assertTrue(new PactRestServiceHelper("https://app-t6.adeo.no/veilarbperson" + "/internal/isAlive").withISSO().isAlive());
    }
}