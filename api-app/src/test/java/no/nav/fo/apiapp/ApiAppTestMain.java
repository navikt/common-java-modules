package no.nav.fo.apiapp;

import no.nav.fo.apiapp.rest.JettyTestUtils;
import no.nav.testconfig.ApiAppTest;

public class ApiAppTestMain {

    public static void main(String[] args) throws Exception {
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder()
                .applicationName("api-app")
                .build()
        );
        JettyTestUtils.setupContext();
        ApiAppMain.main();
    }

}
