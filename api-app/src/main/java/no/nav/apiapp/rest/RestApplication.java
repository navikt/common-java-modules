package no.nav.apiapp.rest;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.Konfigurator;
import no.nav.json.JsonProvider;
import org.springframework.context.ApplicationContext;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static no.nav.json.DateConfiguration.parameterConverterProvider;

public class RestApplication extends Application {

    private final ApplicationContext applicationContext;
    private final ApiApplication apiApplication;
    private final Konfigurator konfigurator;

    public RestApplication(ApplicationContext applicationContext, ApiApplication apiApplication, Konfigurator konfigurator) {
        this.applicationContext = applicationContext;
        this.apiApplication = apiApplication;
        this.konfigurator = konfigurator;
    }

    @Override
    public Set<Object> getSingletons() {
        HashSet<Object> singeltons = new HashSet<>();
        ExceptionMapper exceptionMapper = new ExceptionMapper();
        singeltons.addAll(asList(
                new JsonProvider(konfigurator.getObjectMapper()),
                new AlltidJsonFilter(),
                new ReadExceptionHandler(exceptionMapper),
                exceptionMapper,
                new NavMetricsBinder(),
                parameterConverterProvider(),
                new PingResource(),
                new SwaggerResource(apiApplication)
        ));
        singeltons.addAll(getBeansWithAnnotation(Provider.class));
        singeltons.addAll(getBeansWithAnnotation(Path.class));
        return singeltons;
    }

    private Collection<Object> getBeansWithAnnotation(Class<? extends Annotation> aClass) {
        return applicationContext.getBeansWithAnnotation(aClass).values();
    }

}
