package no.nav.common.utils;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static no.nav.common.utils.NaisUtils.CONFIG_MAPS_BASE_PATH_PROPERTY_NAME;
import static no.nav.common.utils.NaisUtils.SECRETS_BASE_PATH_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NaisUtilsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Before
    public void setup() {
        System.setProperty(SECRETS_BASE_PATH_PROPERTY_NAME, tmp.getRoot().getAbsolutePath());
    }

    @Test
    @SneakyThrows
    public void readFileContent() {
        createFolder("foo", "bar");
        writeFile("foo/bar/baz", "the content");
        String fileContent = NaisUtils.getFileContent(tempPath("foo/bar/baz"));
        assertThat(fileContent).isEqualTo("the content");
    }

    @Test
    public void readCredentialsWithDefaultFileNames() {
        createFolder("creds");
        writeFile("creds/username", "the username");
        writeFile("creds/password", "the password");
        Credentials credentials = NaisUtils.getCredentials("creds");
        assertThat(credentials.username).isEqualTo("the username");
        assertThat(credentials.password).isEqualTo("the password");
    }

    @Test
    public void readCredentialsWithCustomFileNames() {
        createFolder("creds");
        writeFile("creds/un", "the username");
        writeFile("creds/pw", "the password");
        Credentials credentials = NaisUtils.getCredentials("creds", "un", "pw");
        assertThat(credentials.username).isEqualTo("the username");
        assertThat(credentials.password).isEqualTo("the password");
    }

    @Test
    public void readConfigMap() {
        createFolder("configMaps", "configMap");
        System.setProperty(CONFIG_MAPS_BASE_PATH_PROPERTY_NAME, tempPath("configMaps"));

        writeFile("configMaps/configMap/KEY_1", "VALUE 1");
        writeFile("configMaps/configMap/KEY_2", "VALUE 2");
        writeFile("configMaps/configMap/KEY_3", "VALUE 3");

        Map<String, String> configMap = NaisUtils.readConfigMap("configMap");


        assertThat(configMap).containsOnlyKeys("KEY_1", "KEY_2", "KEY_3");
        assertThat(configMap.get("KEY_1")).isEqualTo("VALUE 1");
        assertThat(configMap.get("KEY_2")).isEqualTo("VALUE 2");
        assertThat(configMap.get("KEY_3")).isEqualTo("VALUE 3");
    }

    @Test
    public void cherryPickFromConfigMap() {
        createFolder("configMaps", "configMap");
        System.setProperty(CONFIG_MAPS_BASE_PATH_PROPERTY_NAME, tempPath("configMaps"));

        writeFile("configMaps/configMap/KEY_1", "VALUE 1");
        writeFile("configMaps/configMap/KEY_2", "VALUE 2");
        writeFile("configMaps/configMap/KEY_3", "VALUE 3");

        Map<String, String> configMap = NaisUtils.readConfigMap("configMap", "KEY_1", "KEY_3");

        assertThat(configMap).containsOnlyKeys("KEY_1", "KEY_3");
        assertThat(configMap.get("KEY_1")).isEqualTo("VALUE 1");
        assertThat(configMap.get("KEY_3")).isEqualTo("VALUE 3");
    }

    @Test
    public void cherryPickFromConfigMapFailsWhenKeyIsNotFound() {
        createFolder("configMaps", "configMap");
        System.setProperty(CONFIG_MAPS_BASE_PATH_PROPERTY_NAME, tempPath("configMaps"));

        writeFile("configMaps/configMap/KEY_1", "VALUE 1");
        writeFile("configMaps/configMap/KEY_2", "VALUE 2");
        writeFile("configMaps/configMap/KEY_3", "VALUE 3");


        assertThatThrownBy(() -> NaisUtils.readConfigMap("configMap", "KEY_1", "KEY_4"))
                .hasMessage("Fant ikke key KEY_4 i config map configMap");
    }


    @SneakyThrows
    private void createFolder(String... folderNames) {
        tmp.newFolder(folderNames);
    }

    @SneakyThrows
    private void writeFile(String path, String content) {
        Files.write(Paths.get(tempPath(path)), Collections.singletonList(content));
    }

    private String tempPath(String path) {
        return tmp.getRoot().getAbsolutePath() + "/" + path;
    }
}
