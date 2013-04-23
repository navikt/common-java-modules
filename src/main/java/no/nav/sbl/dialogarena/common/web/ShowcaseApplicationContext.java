package no.nav.sbl.dialogarena.common.web;

import no.nav.sbl.dialogarena.common.tilbakemelding.service.Epostsender;
import no.nav.sbl.dialogarena.common.tilbakemelding.service.TilbakemeldingService;

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
    public TilbakemeldingService tilbakemeldingService() {
        final int port = 25;
        return new Epostsender("127.0.0.1", port, "showcase", "showcase@nav.no");
    }

}
