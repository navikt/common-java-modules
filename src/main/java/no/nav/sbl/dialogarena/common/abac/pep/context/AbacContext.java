package no.nav.sbl.dialogarena.common.abac.pep.context;

import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.PepImpl;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.abac.pep.service.LdapService;
import org.springframework.context.annotation.*;

@Configuration
@Import({ServiceContext.class, ServiceProviderContext.class})
public class AbacContext {

    @Bean
    public Pep pep(LdapService ldapService, AbacService abacService) {
        return new PepImpl(ldapService, abacService);
    }
}
