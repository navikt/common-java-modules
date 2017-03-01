package no.nav.sbl.dialogarena.common.abac.pep.context;

import no.nav.sbl.dialogarena.common.abac.pep.service.Ldap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ServiceProviderContext {

    @Bean
    Ldap ldap() {
        return new Ldap();
    }
}
