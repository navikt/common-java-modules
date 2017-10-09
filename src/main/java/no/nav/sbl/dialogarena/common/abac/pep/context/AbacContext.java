package no.nav.sbl.dialogarena.common.abac.pep.context;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import no.nav.sbl.dialogarena.common.abac.pep.AbacHelsesjekker;
import no.nav.sbl.dialogarena.common.abac.pep.PepImpl;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static net.sf.ehcache.config.PersistenceConfiguration.Strategy.LOCALTEMPSWAP;
import static net.sf.ehcache.store.MemoryStoreEvictionPolicy.LRU;

@Configuration
@Import({
        ServiceContext.class,
        AbacHelsesjekker.Ping.class,
        AbacHelsesjekker.SelfTest.class
})
public class AbacContext {

    public static final String ASK_FOR_PERMISSION = "askForPermission";
    public static final CacheConfiguration ABAC_CAHE = new CacheConfiguration(ASK_FOR_PERMISSION, 10000)
            .memoryStoreEvictionPolicy(LRU)
            .timeToIdleSeconds(3600)
            .timeToLiveSeconds(3600)
            .persistence(new PersistenceConfiguration().strategy(LOCALTEMPSWAP));

    @Bean
    public PepImpl pep(AbacService abacService) {
        return new PepImpl(abacService);
    }

}
