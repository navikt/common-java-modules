package no.nav.common.abac.context;

import no.nav.common.abac.service.AbacService;
import no.nav.common.abac.service.AbacServiceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceContext {

    @Bean
    public AbacService abacService(AbacServiceConfig abacServiceConfig) {
        return new AbacService(abacServiceConfig);
    }

}
