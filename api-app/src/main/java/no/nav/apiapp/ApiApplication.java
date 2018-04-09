package no.nav.apiapp;

import no.nav.apiapp.config.ApiAppConfigurator;

import javax.servlet.ServletContext;

public interface ApiApplication {

    String DEFAULT_API_PATH = "/api/";
    boolean STSHelsesjekkDefault = true;

    String getApplicationName();

    Sone getSone();

    default String getApiBasePath(){
        return DEFAULT_API_PATH;
    }

    default boolean brukSTSHelsesjekk() {
        return STSHelsesjekkDefault;
    }

    default boolean brukContextPath() {
        return true;
    }

    default void startup(@SuppressWarnings("unused") ServletContext servletContext){}

    default void shutdown(@SuppressWarnings("unused") ServletContext servletContext){}

    enum Sone{
        FSS,
        SBS
    }

    interface NaisApiApplication extends ApiApplication {

        void configure(ApiAppConfigurator apiAppConfigurator);

    }

}
