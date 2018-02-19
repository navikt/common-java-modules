package no.nav.fo.pact;

import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

@Ignore
public class FOConsumerServiceTest {

    @Before
    public void setUp() {
        SSLTestUtils.disableCertificateChecks();
    }

    @Test
    public void isNais_Alive() {
        assertTrue(
                new FOConsumerService("https://pact-broker.nais.preprod.local")
                .withBasicAuth(System.getenv("PACTUSER"), System.getenv("PACTPASSWORD"))
                .isAlive());
    }

    @Test
    public void isCloud_Alive() {
        assertTrue(new FOConsumerService("https://app-t6.adeo.no/veilarbperson" + FOEndpoints.IS_ALIVE_ENDPOINT).withISSO().isAlive());
    }
}