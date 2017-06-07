package no.nav.sbl.dialogarena.common.abac.pep.context;

import no.nav.sbl.dialogarena.common.abac.pep.service.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceContext {

    @Bean
    public LdapService ldapService(Ldap ldap) {
        return new LdapService(ldap);
    }

    @Bean
    public AbacService abacService(Abac abac, CloseableHttpClient httpClient) {
        return new AbacService(abac, httpClient);
    }
}
