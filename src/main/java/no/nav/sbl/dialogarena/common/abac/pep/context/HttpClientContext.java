package no.nav.sbl.dialogarena.common.abac.pep.context;

import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.System.getProperty;
import static no.nav.sbl.dialogarena.common.abac.pep.Utils.getApplicationProperty;

@Configuration
public class HttpClientContext {

    @Bean
    public CloseableHttpClient createHttpClient() throws NoSuchFieldException {
        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager())
                .setDefaultRequestConfig(createConfigForTimeout())
                .setDefaultCredentialsProvider(addSystemUserToRequest())
                .build();
    }

    static HttpClientConnectionManager connectionManager() {
        PoolingHttpClientConnectionManager clientConnectionManager = new PoolingHttpClientConnectionManager();
        clientConnectionManager.setMaxTotal(1024);
        clientConnectionManager.setDefaultMaxPerRoute(1024);
        return clientConnectionManager;
    }

    static CredentialsProvider addSystemUserToRequest() throws NoSuchFieldException {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(
                getApplicationProperty(CredentialConstants.SYSTEMUSER_USERNAME),
                getApplicationProperty(CredentialConstants.SYSTEMUSER_PASSWORD));
        provider.setCredentials(AuthScope.ANY, credentials);
        return provider;
    }


    static RequestConfig createConfigForTimeout() {
        return RequestConfig.custom()
                .setConnectTimeout(getConnectionTimeout())
                .setConnectionRequestTimeout(getConnectionTimeout())
                .setSocketTimeout(getReadTimeout())
                .build();
    }

    private static int getConnectionTimeout() {
        final String propertyConnectionTimeout = getProperty("abac.bibliotek.connectionTimeout");
        final int defaultConnectionTimeout = 500;
        return StringUtils.isNotBlank(propertyConnectionTimeout) ?
                Integer.parseInt(propertyConnectionTimeout) : defaultConnectionTimeout;
    }

    private static int getReadTimeout() {
        final String propertyConnectionTimeout = getProperty("abac.bibliotek.readTimeout");
        final int defaultConnectionTimeout = 1500;
        return StringUtils.isNotBlank(propertyConnectionTimeout) ?
                Integer.parseInt(propertyConnectionTimeout) : defaultConnectionTimeout;
    }
}
