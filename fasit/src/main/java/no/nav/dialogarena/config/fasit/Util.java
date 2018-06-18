package no.nav.dialogarena.config.fasit;

import lombok.SneakyThrows;
import no.nav.sbl.rest.RestUtils;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

class Util {

    @SneakyThrows
    public static Properties loadProperties(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            Properties properties = new Properties();
            properties.load(fileInputStream);
            return properties;
        }
    }

}
