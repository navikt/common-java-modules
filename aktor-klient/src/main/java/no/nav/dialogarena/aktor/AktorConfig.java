package no.nav.dialogarena.aktor;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static java.lang.System.getProperty;
import static net.sf.ehcache.config.PersistenceConfiguration.Strategy.LOCALTEMPSWAP;
import static net.sf.ehcache.store.MemoryStoreEvictionPolicy.LRU;
import static no.nav.util.sbl.EnvironmentUtils.getRequiredProperty;

@Configuration
@Import({AktorHelsesjekk.class, AktorServiceImpl.class})
public class AktorConfig {

    public static final String AKTOER_ENDPOINT_URL = "aktoer.endpoint.url";
    private static final String AKTOER_ENDPOINT_URL_NAIS = "AKTOER_V2_ENDPOINTURL";

    protected static final String FNR_FROM_AKTOR_ID = "fnrFromAktorId";
    protected static final String AKTOR_ID_FROM_FNR = "aktorIdFromFnr";

    public static final CacheConfiguration AKTOR_ID_FROM_FNR_CACHE = setupCache(AKTOR_ID_FROM_FNR);
    public static final CacheConfiguration FNR_FROM_AKTOR_ID_CACHE = setupCache(FNR_FROM_AKTOR_ID);

    @Bean
    public AktoerV2 aktoerV2() {
        return new CXFClient<>(AktoerV2.class)
                .address(getAktorEndpointUrl())
                .configureStsForSystemUserInFSS()
                .withMetrics()
                .build();
    }

    private static CacheConfiguration setupCache(String name) {
        return new CacheConfiguration(name, 100000)
                .memoryStoreEvictionPolicy(LRU)
                .timeToIdleSeconds(86400)
                .timeToLiveSeconds(86400)
                .persistence(new PersistenceConfiguration().strategy(LOCALTEMPSWAP));
    }

    static String getAktorEndpointUrl() {
        return getRequiredProperty(AKTOER_ENDPOINT_URL, AKTOER_ENDPOINT_URL_NAIS);
    }

}
