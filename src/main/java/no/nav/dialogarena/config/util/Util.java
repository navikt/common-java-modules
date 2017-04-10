package no.nav.dialogarena.config.util;

import lombok.SneakyThrows;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class Util {

    private static final SslContextFactory SSL_CONTEXT_FACTORY = new SslContextFactory();
    private static final Logger LOG = getLogger(Util.class);

    public static void setProperty(String propertyName, String value) {
        LOG.info("property {} = {}", propertyName, value);
        System.setProperty(propertyName, value);
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

    @FunctionalInterface
    public interface With<T, R> {

        R with(T t) throws Exception;

    }

}
