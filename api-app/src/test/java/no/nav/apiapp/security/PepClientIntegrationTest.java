package no.nav.apiapp.security;

import no.nav.fasit.FasitUtils;
import no.nav.fasit.ServiceUser;
import no.nav.fasit.dto.RestService;
import no.nav.sbl.dialogarena.common.abac.pep.PepImpl;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig;

import static no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType.VeilArbPerson;

public class PepClientIntegrationTest implements PepClientTester {

    @Override
    public PepClient getPepClient() {
        ServiceUser srvveilarbdemo = FasitUtils.getServiceUser("srvveilarbdemo", "veilarbdemo");
        RestService abacEndpoint = FasitUtils.getRestService("abac.pdp.endpoint", srvveilarbdemo.getEnvironment());
        AbacServiceConfig abacServiceConfig = AbacServiceConfig.builder()
                .username(srvveilarbdemo.getUsername())
                .password(srvveilarbdemo.getPassword())
                .endpointUrl(abacEndpoint.getUrl())
                .build();
        PepImpl pep = new PepImpl(new AbacService(abacServiceConfig));
        return new PepClient(pep, "veilarb", VeilArbPerson);
    }

}
