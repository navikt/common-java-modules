package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.metrics.MetodeTimer;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.SocketException;

import static java.lang.System.getProperty;
import static org.slf4j.LoggerFactory.getLogger;

public class Abac {
    private static final Logger LOG = getLogger(Abac.class);


    HttpResponse isAuthorized(RequestConfig config, HttpPost httpPost, CredentialsProvider credentialsProvider) throws NoSuchFieldException, IOException {
        if (isSimulateConnectionProblem()) {
            throw new SocketException("Simulating connection problem");
        }
        final CloseableHttpClient httpClient = createHttpClient(config, credentialsProvider);

        return doPost(httpPost, httpClient);
    }

    private HttpResponse doPost(HttpPost httpPost, CloseableHttpClient httpClient) throws IOException {
        try {
            return (HttpResponse) MetodeTimer.timeMetode(() -> httpClient.execute(httpPost), "abac-pdp");
        } catch (Throwable throwable) {
            LOG.warn("Problem in metrics library", throwable);
        }
        return httpClient.execute(httpPost);
    }

    private CloseableHttpClient createHttpClient(RequestConfig config, CredentialsProvider credentialsProvider) throws NoSuchFieldException {

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }

    private boolean isSimulateConnectionProblem() {
        final String propertyConnectionProblem = getProperty("abac.bibliotek.simuler.avbrudd");
        return StringUtils.isNotBlank(propertyConnectionProblem) && propertyConnectionProblem.equals("true");
    }
}
