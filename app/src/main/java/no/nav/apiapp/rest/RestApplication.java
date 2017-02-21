package no.nav.apiapp.rest;

import org.springframework.context.ApplicationContext;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class RestApplication extends Application {

    private final ApplicationContext applicationContext;

    public RestApplication(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    @Override
    public Set<Object> getSingletons() {
        HashSet<Object> singeltons = new HashSet<>();
        singeltons.addAll(asList(
                new JsonProvider(),
                new AlltidJsonFilter(),
                new CacheBusterFilter(),
                new ExceptionMapper(),
                new NavMetricsBinder()
        ));
        singeltons.addAll(getBeansWithAnnotation(Provider.class));
        singeltons.addAll(getBeansWithAnnotation(Path.class));
        return singeltons;
    }

    private Collection<Object> getBeansWithAnnotation(Class<? extends Annotation> aClass) {
        return applicationContext.getBeansWithAnnotation(aClass).values();
    }

}
