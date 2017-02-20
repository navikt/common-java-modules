package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.service.LdapService;
import no.nav.sbl.dialogarena.common.abac.pep.service.TilgangService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceContext {

    @Bean
    public LdapService ldapService() {
        return new LdapService();
    }

    @Bean
    public TilgangService tilgangService() {
        return new TilgangService();
    }

    @Bean
    public PdpService pdpService() {
        return new PdpService();
    }
}
