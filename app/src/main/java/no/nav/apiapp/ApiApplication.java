package no.nav.apiapp;

public interface ApiApplication {

    Sone getSone();

    enum Sone{
        FSS,
        SBS
    }

}
