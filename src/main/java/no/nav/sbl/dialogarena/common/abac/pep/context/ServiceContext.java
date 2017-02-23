package no.nav.sbl.dialogarena.common.abac.pep.context;

import no.nav.sbl.dialogarena.common.abac.pep.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceContext {

    private Ldap ldap;

    public ServiceContext(Ldap ldap) {
        this.ldap = ldap;
    }

    @Bean
    public LdapService ldapService() {
        return new LdapService(ldap);
    }

    @Bean
    public AbacService abacService() {
        return new AbacService();
    }
}
