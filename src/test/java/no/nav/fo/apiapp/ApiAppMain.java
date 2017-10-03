package no.nav.fo.apiapp;

import no.nav.apiapp.ApiApp;

import static no.nav.fo.apiapp.StartJetty.JETTY_PORT;

public class ApiAppMain {

    public static final String[] DEFAULT_ARGS = {Integer.toString(JETTY_PORT)};

    public static void main(String[] args) throws Exception {
        ApiApp.startApp(ApplicationConfig.class, DEFAULT_ARGS);
    }

}
