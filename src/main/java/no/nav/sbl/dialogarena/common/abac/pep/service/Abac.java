package no.nav.sbl.dialogarena.common.abac.pep.service;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.net.SocketException;

import static java.lang.System.getProperty;

public class Abac {
    HttpResponse isAuthorized(RequestConfig config, HttpPost httpPost, CredentialsProvider credentialsProvider) throws IOException, NoSuchFieldException {
        if (isSimulateConnectionProblem()) {
            throw new SocketException("Simulating connection problem");
        }
        final CloseableHttpClient httpClient = createHttpClient(config, credentialsProvider);
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
