package no.nav.sbl.dialogarena.common.abac.pep.context;

import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
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

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static no.nav.sbl.dialogarena.common.abac.pep.Utils.getApplicationProperty;

@Configuration
public class HttpClientContext {

    static final String DEFAULT_READ_TIMEOUT = "1500";
    static final String DEFAULT_CONNECTION_TIMEOUT = "500";
    static final String KEY_READ_TIMEOUT = "abac.bibliotek.readTimeout";
    static final String KEY_CONNECTION_TIMEOUT = "abac.bibliotek.connectionTimeout";

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
        int connectionTimeout = parseInt(getProperty(KEY_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT));
		return RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setConnectionRequestTimeout(connectionTimeout)
                .setSocketTimeout(parseInt(getProperty(KEY_READ_TIMEOUT, DEFAULT_READ_TIMEOUT)))
                .build();
    }
}
