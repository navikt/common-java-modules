package no.nav.sbl.dialogarena.common.abac.pep.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
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


    CloseableHttpResponse isAuthorized(CloseableHttpClient client, HttpPost httpPost) throws NoSuchFieldException, IOException {
        if (isSimulateConnectionProblem()) {
            throw new SocketException("Simulating connection problem");
        }

        return doPost(httpPost, client);
    }

    private CloseableHttpResponse doPost(HttpPost httpPost, CloseableHttpClient httpClient) throws IOException {
        try {
            return httpClient.execute(httpPost);
        } catch (Throwable throwable) {
            throw new IOException("Problem calling ABAC", throwable);
        }
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
