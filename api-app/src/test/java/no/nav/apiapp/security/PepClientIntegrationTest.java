package no.nav.apiapp.security;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.sbl.dialogarena.common.abac.pep.PepImpl;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import org.junit.jupiter.api.BeforeAll;

import static no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType.VeilArbPerson;

public class PepClientIntegrationTest implements PepClientTester {

    public static final String EKSEMPEL_APPLIKASJON = "veilarbaktivitet";

    @BeforeAll
    public static void setup() {
        DevelopmentSecurity.setupIntegrationTestSecurity(new DevelopmentSecurity.IntegrationTestConfig(EKSEMPEL_APPLIKASJON));
    }

    @Override
    public PepClient getPepClient() {
        PepImpl pep = new PepImpl(new AbacService());
        return new PepClient(pep, "veilarb", VeilArbPerson);
    }

}
