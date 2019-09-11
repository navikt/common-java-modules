package no.nav.apiapp;

import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.util.sbl.EnvironmentUtils;

import javax.servlet.ServletContext;

public interface ApiApplication {

    String DEFAULT_API_PATH = "/api/";

    default String getApiBasePath(){
        return DEFAULT_API_PATH;
    }

    default String getContextPath() {
        return EnvironmentUtils.requireApplicationName();
    }

    default void startup(@SuppressWarnings("unused") ServletContext servletContext){}

    default void shutdown(@SuppressWarnings("unused") ServletContext servletContext){}

    void configure(ApiAppConfigurator apiAppConfigurator);


}
