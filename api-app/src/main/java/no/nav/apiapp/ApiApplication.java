package no.nav.apiapp;

import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Environment;
import no.nav.sbl.util.EnvironmentUtils;

import javax.servlet.ServletContext;

public interface ApiApplication {

    String DEFAULT_API_PATH = "/api/";
    boolean STSHelsesjekkDefault = true;

    default String getApiBasePath(){
        return DEFAULT_API_PATH;
    }

    default boolean brukSTSHelsesjekk() {
        return STSHelsesjekkDefault;
    }

    default String getContextPath() {
        return EnvironmentUtils.requireApplicationName();
    }

    default void startup(@SuppressWarnings("unused") ServletContext servletContext){}

    default void shutdown(@SuppressWarnings("unused") ServletContext servletContext){}

    void configure(ApiAppConfigurator apiAppConfigurator);


}
