package no.nav.sbl.dialogarena.common.abac.pep.context;

import no.nav.sbl.dialogarena.common.abac.pep.AbacHelsesjekk;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.PepImpl;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.abac.pep.service.LdapService;
import org.springframework.context.annotation.*;

@Configuration
@Import({
        HttpClientContext.class,
        ServiceContext.class,
        ServiceProviderContext.class,
        AbacHelsesjekk.class
})
public class AbacContext {

    @Bean
    public PepImpl pep(LdapService ldapService, AbacService abacService) {
        return new PepImpl(ldapService, abacService);
    }

}
