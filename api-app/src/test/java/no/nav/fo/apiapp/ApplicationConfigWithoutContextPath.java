package no.nav.fo.apiapp;

import no.nav.apiapp.config.ApiAppConfigurator;

public class ApplicationConfigWithoutContextPath extends ApplicationConfig {

    @Override
    public String getContextPath() {
        return "/";
    }

    @Override
    public String getApiBasePath() {
        return "/";
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator.openAmLogin();
    }

}
