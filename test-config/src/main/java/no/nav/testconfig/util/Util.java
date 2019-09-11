package no.nav.testconfig.util;

import lombok.SneakyThrows;
import no.nav.sbl.rest.RestUtils;
import no.nav.util.sbl.StringUtils;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import static no.nav.testconfig.util.Util.Mode.IKKE_OVERSKRIV;
import static no.nav.testconfig.util.Util.Mode.OVERSKRIV;
import static org.slf4j.LoggerFactory.getLogger;

public class Util {

    private static int READ_TIMEOUT = RestUtils.DEFAULT_CONFIG.getReadTimeout();

    private static final Logger LOG = getLogger(Util.class);

    @SuppressWarnings("unused")
    public static void setReadTimeout(int readTimeout) {
        READ_TIMEOUT = readTimeout;
    }

    public static void setProperty(String propertyName, String value) {
        setProperty(propertyName, value, OVERSKRIV);
    }

    public static void setProperty(String propertyName, String value, Mode mode) {
        String property = System.getProperty(propertyName);
        if (mode == IKKE_OVERSKRIV && property != null && property.trim().length() > 0 && !property.equals(value)) {
            LOG.warn("property {} er allerede satt til [{}] (ignorerer [{}])", propertyName, property, value);
        } else {
            LOG.info("property {} = {}", propertyName, value);
            if (StringUtils.notNullOrEmpty(value)) {
                System.setProperty(propertyName, value);
            } else {
                System.clearProperty(propertyName);
            }
        }
    }

    @SneakyThrows
    public static <T> T httpClient(With<Client, T> httpClientConsumer) {
        return RestUtils.withClient(RestUtils.DEFAULT_CONFIG.withReadTimeout(READ_TIMEOUT), httpClientConsumer::withSafe);
    }

    @SneakyThrows
    public static Properties loadProperties(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            Properties properties = new Properties();
            properties.load(fileInputStream);
            return properties;
        }
    }

    public static void setProperties(Properties properties, Mode mode) {
        properties.forEach((k, v) -> setProperty(k.toString(), v.toString(), mode));
    }

    @FunctionalInterface
    public interface With<T, R> {

        @SneakyThrows
        default R withSafe(T t){
            return with(t);
        }

        R with(T t) throws Exception;

    }

    public enum Mode {
        OVERSKRIV,
        IKKE_OVERSKRIV
    }

}
