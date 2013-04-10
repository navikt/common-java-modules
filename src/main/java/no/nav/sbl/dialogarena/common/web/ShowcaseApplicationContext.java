package no.nav.sbl.dialogarena.common.web;

//import org.springframework.context.annotation.Configuration;


import no.nav.sbl.dialogarena.common.tilbakemelding.tilbakemelding.Epostsender;
import org.apache.wicket.protocol.http.WebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShowcaseApplicationContext {


    @Bean
    public WebApplication myApp() {
        return new ShowcaseApplication();
    }

    @Bean
    public Epostsender epostSender(){
        Epostsender epostsender = new Epostsender("127.0.0.1",25);
        return epostsender;

    }

}




