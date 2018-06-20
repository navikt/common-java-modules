package no.nav.testconfig;

import org.junit.Test;

public class ApiAppTestTest {

    @Test
    public void smoketest(){
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder()
                .applicationName(ApiAppTestTest.class.getSimpleName())
                .build()
        );
    }

}