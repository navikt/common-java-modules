package no.nav.apiapp.version;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class VersionServiceTest {

    @Test
    public void getVersions() {
        List<Version> versions = new VersionService().getVersions();
        versions.forEach(v -> {
            assertThat(v.component).isNotEmpty();

            if (!"common".equals(v.component)) {
                assertThat(v.version).isNotEmpty().isNotEqualTo(VersionService.UNKNOWN_VERSION);
            }
        });
    }

}