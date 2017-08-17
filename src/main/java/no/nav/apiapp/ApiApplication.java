package no.nav.apiapp;

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

    enum Sone{
        FSS,
        SBS
    }

}
