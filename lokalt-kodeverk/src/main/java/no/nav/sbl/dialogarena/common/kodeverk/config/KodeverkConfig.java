package no.nav.sbl.dialogarena.common.kodeverk.config;


import no.nav.sbl.dialogarena.common.kodeverk.JsonKodeverk;
import no.nav.sbl.dialogarena.common.kodeverk.Kodeverk;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KodeverkConfig {

    @Bean
    public final Kodeverk kodeverk() {
        return new JsonKodeverk(getClass().getResourceAsStream("/kodeverk.json"));
    }
}