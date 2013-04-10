import no.nav.sbl.dialogarena.common.tilbakemelding.tilbakemelding.EpostKonfigurasjon;
import no.nav.sbl.dialogarena.common.tilbakemelding.tilbakemelding.Epostsender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class TilbakemeldingTestContext {



    @Bean
    public Epostsender epostSender(){
        Epostsender epostsender = new Epostsender("127.0.0.1",25);
        return epostsender;

    }




}
