package no.nav.apiapp.util;

import no.nav.fo.apiapp.ApplicationConfig;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class WarFolderFinderUtilTest {

    @Test
    public void find_local_running_of_apiApp() {
        File file = WarFolderFinderUtil.findPath(ApplicationConfig.class);
        assertThat(file.getAbsoluteFile()).isEqualTo(new File("src/main/webapp").getAbsoluteFile());
    }

    @Test
    public void find_fallback_to_docker_path_if_sources_not_found() {
        File file = WarFolderFinderUtil.findPath(null);
        assertThat(file).isEqualTo(new File("/app"));
    }

}
