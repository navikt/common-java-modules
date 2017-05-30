package no.nav.fo.apiapp;

import no.nav.apiapp.ApiApplication;
import no.nav.fo.apiapp.rest.DatoEksempel;
import no.nav.fo.apiapp.rest.RestEksempel;
import no.nav.fo.apiapp.rest.SwaggerEksempel;
import no.nav.fo.apiapp.security.KreverSesjon;
import no.nav.fo.apiapp.selftest.PingableEksempel;
import no.nav.fo.apiapp.soap.SoapEksempel;
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
    public DatoEksempel datoEksempel(){
        return new DatoEksempel();
    }

    @Bean
    public SoapEksempel soapEksempel() {
        return new SoapEksempel();
    }

    @Bean
    public KreverSesjon kreverSesjon() {
        return new KreverSesjon();
    }

    @Bean
    public SwaggerEksempel swaggerEksempel(){
        return new SwaggerEksempel();
    }

    @Override
    public Sone getSone() {
        return Sone.values()[0];
    }

}
