package no.nav.apiapp;

import javax.servlet.ServletContext;

public interface ApiApplication {

    String DEFAULT_API_PATH = "/api/";
    boolean STSHelsesjekkDefault = true;

    Sone getSone();
    default String getApiBasePath(){
        return DEFAULT_API_PATH;
    }

    default boolean brukSTSHelsesjekk() {
        return STSHelsesjekkDefault;
    }

    default void startup(ServletContext servletContext){}

    default void shutdown(ServletContext servletContext){}

    enum Sone{
        FSS,
        SBS
    }

}
