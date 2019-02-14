package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.fasit.FasitUtils;
import no.nav.fasit.ServiceUser;
import no.nav.fasit.dto.RestService;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig;
import no.nav.sbl.dialogarena.test.FasitAssumption;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.util.EnvironmentUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;
import static org.assertj.core.api.Assertions.assertThat;

public class AbacHelsesjekkerIntegrationTest {

    private static final String APPLICATION_NAME = "veilarbdemo";

    private static AbacServiceConfig abacServiceConfig;

    @BeforeClass
    public static void setup() {
        FasitAssumption.assumeFasitAccessible();
        ServiceUser serviceUser = FasitUtils.getServiceUser("srvveilarbdemo", APPLICATION_NAME);
        RestService restService = FasitUtils.getRestService("abac.pdp.endpoint", serviceUser.environment);
        abacServiceConfig = AbacServiceConfig.builder()
                .username(serviceUser.username)
                .password(serviceUser.password)
                .endpointUrl(restService.getUrl())
                .build();
    }

    @Test
    public void ping() {
        setTemporaryProperty(EnvironmentUtils.APP_NAME_PROPERTY_NAME, APPLICATION_NAME, () -> {
            AbacHelsesjekker.Ping pingHelsesjekk = new AbacHelsesjekker.Ping(new PepImpl(new AbacService(abacServiceConfig)), abacServiceConfig);
            Pingable.Ping ping = pingHelsesjekk.ping();
            assertThat(ping.erVellykket()).isTrue();
        });
    }

    @Test
    public void selfTest() {
        AbacHelsesjekker.SelfTest selfTestHelsesjekk = new AbacHelsesjekker.SelfTest(abacServiceConfig);
        Pingable.Ping ping = selfTestHelsesjekk.ping();
        assertThat(ping.erVellykket()).isTrue();
    }

}