package no.nav.sbl.dialogarena.common.abac.pep.context;

import no.nav.sbl.dialogarena.common.abac.pep.AbacHelsesjekker;
import no.nav.sbl.dialogarena.common.abac.pep.PepImpl;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        ServiceContext.class,
        AbacHelsesjekker.Ping.class,
        AbacHelsesjekker.SelfTest.class
})
public class AbacContext {

    @Bean
    public PepImpl pep(AbacService abacService) {
        return new PepImpl(abacService);
    }

}
