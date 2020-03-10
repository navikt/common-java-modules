package no.nav.testconfig;

import org.junit.Test;

import static no.nav.testconfig.ApiAppTest.DEFAULT_ENVIRONMENT;

public class ApiAppTestTest {

    @Test
    public void smoketest(){
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder()
                .applicationName(ApiAppTestTest.class.getSimpleName())
                .environment(DEFAULT_ENVIRONMENT)
                .build()
        );
    }

}