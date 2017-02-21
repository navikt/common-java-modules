package no.nav.fo.apiapp;

import no.nav.apiapp.ApiApplication;
import no.nav.fo.apiapp.rest.RestEksempel;
import no.nav.fo.apiapp.security.KreverSesjon;
import no.nav.fo.apiapp.selftest.PingableEksempel;
import no.nav.sbl.dialogarena.types.Pingable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig implements ApiApplication {

    @Bean
    public Pingable pingable() {
        return new PingableEksempel();
    }

    @Bean
    public RestEksempel restEksempel() {
        return new RestEksempel();
    }

    @Bean
    public KreverSesjon kreverSesjon() {
        return new KreverSesjon();
    }

    @Override
    public Sone getSone() {
        return Sone.values()[0];
    }

}
