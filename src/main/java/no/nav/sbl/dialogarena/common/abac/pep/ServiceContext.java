package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.abac.pep.service.LdapService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceContext {

    @Bean
    public LdapService ldapService() {
        return new LdapService();
    }

    @Bean
    public AbacService abacService() {
        return new AbacService();
    }
}
