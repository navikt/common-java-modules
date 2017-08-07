package no.nav.apiapp;

public interface ApiApplication {

    String DEFAULT_API_PATH = "/api/";

    Sone getSone();
    default String getApiBasePath(){
        return DEFAULT_API_PATH;
    }

    enum Sone{
        FSS,
        SBS
    }

}
