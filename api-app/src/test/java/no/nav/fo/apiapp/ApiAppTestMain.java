package no.nav.fo.apiapp;

import no.nav.fo.apiapp.rest.JettyTestUtils;
import no.nav.testconfig.ApiAppTest;

import static no.nav.testconfig.ApiAppTest.DEFAULT_ENVIRONMENT;

public class ApiAppTestMain {

    public static void main(String[] args) {
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder()
                .applicationName("api-app")
                .environment(DEFAULT_ENVIRONMENT)
                .build()
        );
        JettyTestUtils.setupContext();
        ApiAppMain.main();
    }

}
