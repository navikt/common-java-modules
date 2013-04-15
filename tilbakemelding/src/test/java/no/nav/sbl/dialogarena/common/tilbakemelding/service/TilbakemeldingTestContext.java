package no.nav.sbl.dialogarena.common.tilbakemelding.service;
import no.nav.modig.lang.util.Ports;
import no.nav.sbl.dialogarena.common.tilbakemelding.service.Epostsender;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TilbakemeldingTestContext {


    @Bean
    public Epostsender epostSender(){
        Epostsender epostsender = new Epostsender("127.0.0.1", smtpPort(), "test", "tilbakemeldinger@nav.no");
        return epostsender;
    }

    @Bean
    public Integer smtpPort() {
        return Ports.getFirstAvailablePortFrom(49999);
    }


}
