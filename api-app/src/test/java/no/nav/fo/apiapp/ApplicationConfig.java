package no.nav.fo.apiapp;

import com.zaxxer.hikari.HikariDataSource;
import no.nav.apiapp.ApiApplication.NaisApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.apiapp.db.DatabaseExample;
import no.nav.apiapp.servlet.ForwardServletExample;
import no.nav.apiapp.servlet.IncludeServletExample;
import no.nav.fo.apiapp.rest.*;
import no.nav.fo.apiapp.security.KreverSesjon;
import no.nav.fo.apiapp.selftest.PingableEksempel;
import no.nav.fo.apiapp.soap.SoapEksempel;
import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.jdbc.DataSourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.servlet.ServletContext;
import java.util.Random;
import java.util.stream.Stream;

import static no.nav.apiapp.ServletUtil.leggTilServlet;

@Configuration
@Import({
        AbacContext.class,
        InjectionEksempel.class,
        EksempelService.class,
        DatabaseExample.class
})
public class ApplicationConfig implements NaisApiApplication {

    @Bean
    public Pingable pingable() {
        return new PingableEksempel();
    }

    @Bean
    public RestEksempel restEksempel() {
        return new RestEksempel();
    }

    @Bean
    public ServerEksempel serverEksempel() {
        return new ServerEksempel();
    }

    @Bean
    public EnumEksempel enumEksempel() {
        return new EnumEksempel();
    }

    @Bean
    public DatoEksempel datoEksempel() {
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
    public SwaggerEksempel swaggerEksempel() {
        return new SwaggerEksempel();
    }

    @Bean
    public InterfaceEksempelImpl interfaceEksempel() {
        return new InterfaceEksempelImpl();
    }

    @Bean
    public OpenAmEksempel openAmEksempel() {
        return new OpenAmEksempel();
    }

    @Bean
    public RedirectEksempel redirectEksempel() {
        return new RedirectEksempel();
    }

    @Bean
    public FeedController feedController() {
        FeedController feedController = new FeedController();
        FeedProducer.FeedProducerBuilder<Integer> feedProducerBuilder = FeedProducer.<Integer>builder().provider((id, pageSize) -> streamTilfeldigInt());
        feedController.addFeed("tilfeldigetall", feedProducerBuilder.build());
        feedController.addFeed("beskyttetStream", feedProducerBuilder.authorizationModule((a) -> false).build());
        return feedController;
    }

    @Override
    public void startup(ServletContext servletContext) {
        leggTilServlet(servletContext, ForwardServletExample.class, "/forward");
        leggTilServlet(servletContext, IncludeServletExample.class, "/include");
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator.customizeJetty(jetty -> {
            jetty.context.setDescriptor("my custom descriptor");
        });

        if (!JettyTest.DISABLE_AUTH) {
            HikariDataSource hikariDataSource = DataSourceFactory.dataSource()
                    .url("jdbc:h2:mem:")
                    .username("sa")
                    .password("")
                    .build();

            apiAppConfigurator.database(hikariDataSource)
//                .azureADB2CLogin();
//                .samlLogin()
                    .sts()
                    .openAmLogin();

        }
    }

    private static Stream<FeedElement<Integer>> streamTilfeldigInt() {
        return Stream.of(new Random().nextInt()).map(i -> new FeedElement<Integer>().setId(Integer.toString(i)).setElement(i));
    }


}
