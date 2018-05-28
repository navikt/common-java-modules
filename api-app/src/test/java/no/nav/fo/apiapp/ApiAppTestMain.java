package no.nav.fo.apiapp;

import no.nav.testconfig.ApiAppTest;

public class ApiAppTestMain {

    public static void main(String[] args) throws Exception {
        ApiAppTest.setupTestContext();
        JettyTest.setupContext();
        ApiAppMain.main();
    }

}
