package no.nav.common.abac.context;

import net.sf.ehcache.config.CacheConfiguration;
import no.nav.common.abac.AbacHelsesjekker;
import no.nav.common.abac.PepImpl;
import no.nav.common.abac.domain.request.Request;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.exception.AbacException;
import no.nav.common.abac.service.AbacService;
import no.nav.common.abac.service.AbacServiceConfig;
import no.nav.common.auth.SubjectHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static java.util.Arrays.asList;
import static net.sf.ehcache.store.MemoryStoreEvictionPolicy.LRU;

@Configuration
@Import({
        ServiceContext.class,
        AbacHelsesjekker.class,
})
public class AbacContext {

    public static final String ASK_FOR_PERMISSION = "askForPermission";
    public static final CacheConfiguration ABAC_CACHE = new CacheConfiguration(ASK_FOR_PERMISSION, 10000)
            .memoryStoreEvictionPolicy(LRU)
            .timeToIdleSeconds(3600)
            .timeToLiveSeconds(3600);

    @Bean
    public PepImpl pep(AbacService abacService) {
        return new PepImpl(abacService);
    }

    @Bean
    public AbacServiceConfig abacServiceConfig() {
        return AbacServiceConfig.readFromSystemVariables();
    }

    @Bean
    public KeyGenerator abacKeyGenerator() {
        return (target, method, params) -> {
            for (Object o : params) {
                if (o instanceof XacmlRequest) {
                    Request request = ((XacmlRequest) o).getRequest();
                    return asList(SubjectHandler.getIdent().orElse(null),
                            request.getAccessSubject(),
                            request.getAction(),
                            request.getResource());
                }
            }
            throw new AbacException("Cache nøkkel må være et gyldig XacmlRequest object");
        };
    }

}
