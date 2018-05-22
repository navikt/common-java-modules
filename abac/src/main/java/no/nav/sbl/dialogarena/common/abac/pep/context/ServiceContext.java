package no.nav.sbl.dialogarena.common.abac.pep.context;

import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceContext {

    @Bean
    public AbacService abacService(AbacServiceConfig abacServiceConfig) {
        return new AbacService(abacServiceConfig);
    }

}
