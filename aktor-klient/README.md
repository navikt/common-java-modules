# Aktør klient

Dette er et lite bibliotek som lar deg enkelt integrere mot Aktoer_V2 tjenesten.
* Biblioteket tar seg av oppsett av tjenesten med CXF.
* Helsesjekk legges automatisk til i applikasjonen din.
* Kall mot tjenesten caches automatisk, men du må selv configurere en CacheManager.

## Hvordan ta i bruk.

#### Dependency

Versjonering av aktor-klient tas hånd om av no.nav.sbl:bom, Alt du trenger er å legge til en dependency

```
<dependency>
    <groupId>no.nav.common</groupId>
    <artifactId>aktor-klient</artifactId>
</dependency>
```

#### Caching

Hvis du ikke allerede har en CacheManager i applikasjonen din kan den settes opp på følgende måte

```
// Andre imports

import static no.nav.dialogarena.aktor.AktorConfig.AKTOR_ID_FROM_FNR_CACHE;
import static no.nav.dialogarena.aktor.AktorConfig.FNR_FROM_AKTOR_ID_CACHE;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        config.addCache(FNR_FROM_AKTOR_ID_CACHE);
        config.addCache(AKTOR_ID_FROM_FNR_CACHE);
        return new EhCacheCacheManager(net.sf.ehcache.CacheManager.newInstance(config));
    }
}
```

#### Exsempel på bruk

```
public class MyService {

    @Inject
    AktorService aktorService;

    public void someMethod(String fnr) {
        Optional<String> aktorId = aktorService.getAktorId(fnr);

        // 8< -----------------

    }

```

For at AktorService skal være tilgjengelig for injection må Spring vite hvor den finnes.
Legg til en @Import i en av @Configuration klassene dine: ```@Import({AktorConfig.class})```.
Alternativt, hvis du er glad i ComponentScan så pass på at pakken no.nav.dialogarena.aktor er med.