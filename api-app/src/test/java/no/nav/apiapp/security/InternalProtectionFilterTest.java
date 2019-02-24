package no.nav.apiapp.security;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class InternalProtectionFilterTest {

    private InternalProtectionFilter internalProtectionFilter = new InternalProtectionFilter(Arrays.asList(
            "safe.domain.com",
            "local.domain",
            ".test.domain"
    ));

    @Test
    public void isAllowedAccessToInternal() {
        assertNoAccess("myapp.nav.no");
        assertNoAccess("tjenester.nav.no");
        assertNoAccess("evil.com");
        assertNoAccess("174.148.185.236");

        assertAccess("192.168.1.1");
        assertAccess("192.168.255.255");
        assertAccess("localhost");

        assertAccess("safe.domain.com");
        assertAccess("myapp.safe.domain.com");
        assertAccess("myapp.local.domain");
        assertAccess("myapp.test.domain");
    }

    private void assertNoAccess(String hostName) {
        assertThat(internalProtectionFilter.isAllowedAccessToInternal(hostName))
                .describedAs(hostName)
                .isFalse();
    }

    private void assertAccess(String hostName) {
        assertThat(internalProtectionFilter.isAllowedAccessToInternal(hostName))
                .describedAs(hostName)
                .isTrue();
    }

}