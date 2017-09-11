package no.nav.dialogarena.config.util;

import lombok.SneakyThrows;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import static no.nav.dialogarena.config.util.Util.Mode.IKKE_OVERSKRIV;
import static no.nav.dialogarena.config.util.Util.Mode.OVERSKRIV;
import static org.slf4j.LoggerFactory.getLogger;

public class Util {

    private static final SslContextFactory SSL_CONTEXT_FACTORY = new SslContextFactory();
    private static final Logger LOG = getLogger(Util.class);

    public static void setProperty(String propertyName, String value) {
        setProperty(propertyName, value, OVERSKRIV);
    }

    public static void setProperty(String propertyName, String value, Mode mode) {
        String property = System.getProperty(propertyName);
        if (mode == IKKE_OVERSKRIV && property != null && property.trim().length() > 0) {
            LOG.warn("property {} er allerede satt til {}", propertyName, property);
        } else {
            LOG.info("property {} = {}", propertyName, value);
            System.setProperty(propertyName, value);
        }
    }

    @SneakyThrows
    public static <T> T httpClient(With<HttpClient, T> httpClientConsumer) {
        HttpClient httpClient = new HttpClient(SSL_CONTEXT_FACTORY);
        httpClient.setFollowRedirects(false);
        try {
            httpClient.start();
            return httpClientConsumer.with(httpClient);
        } finally {
            httpClient.stop();
        }
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

        R with(T t) throws Exception;

    }

    public enum  Mode {
        OVERSKRIV,
        IKKE_OVERSKRIV
    }

}
