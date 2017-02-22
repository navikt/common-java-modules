package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.abac.pep.service.LdapService;
import org.springframework.context.annotation.*;

@Configuration
@Import({ServiceContext.class, ServiceProviderContext.class})
public class AbacContext {

    private final LdapService ldapService;
    private final AbacService abacService;

    public AbacContext(LdapService ldapService, AbacService abacService) {
        this.ldapService = ldapService;
        this.abacService = abacService;
    }

    @Bean
    public Pep pep() {
        return new Pep(ldapService, abacService);
    }
}
