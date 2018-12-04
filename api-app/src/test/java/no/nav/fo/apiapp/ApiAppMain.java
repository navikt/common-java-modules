package no.nav.fo.apiapp;

import no.nav.apiapp.ApiApp;

public class ApiAppMain {

    public static final int JETTY_PORT = 8765;
    public static final String[] DEFAULT_ARGS = {Integer.toString(JETTY_PORT)};

    public static void main(String... args) {
        ApiApp.runApp(ApplicationConfig.class, DEFAULT_ARGS);
    }

}
